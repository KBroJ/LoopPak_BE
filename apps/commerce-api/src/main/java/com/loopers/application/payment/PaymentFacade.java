package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.*;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.interfaces.api.payment.PgCallbackRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PgPaymentService pgPaymentService;

    private final OrderRepository orderRepository;
    private final PointRepository pointRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResult processPayment(
        Long userId, Long orderId, long amount,
        PaymentType paymentType, PaymentMethod paymentMethod
    ) {
        log.info("결제 처리 시작 - userId: {}, orderId: {}, amount: {}, paymentType: {}", userId, orderId, amount, paymentType);
        try {
            switch (paymentType) {
                case POINT -> {
                    log.info("포인트 결제 처리");
                    return processPointPayment(userId, amount);
                }
                case CARD -> {
                    log.info("카드 결제 처리");
                    return processCardPayment(userId, orderId, amount, paymentMethod);
                }
                default -> throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 결제 방식입니다.");
            }
        } catch (CoreException e) {
            log.error("CoreException 발생 - paymentType: {}, error: {}", paymentType, e.getMessage(), e);
            // 포인트 결제 예외는 그대로 던짐
            if (paymentType == PaymentType.POINT) {
                throw e;
            }
            return PaymentResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("일반 Exception 발생 - paymentType: {}, error: {}", paymentType, e.getMessage(), e);
            return PaymentResult.failure("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional  // 포인트 결제용 트랜잭션 (즉시 완료)
    private PaymentResult processPointPayment(Long userId, long amount) {
        Point userPoint = pointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 포인트 정보를 찾을 수 없습니다."));
        userPoint.use(amount);  // 잔액 부족시 BAD_REQUEST 발생
        return PaymentResult.pointSuccess();
    }

    @Transactional  // 카드 결제용 트랜잭션 (PENDING 저장까지만)
    private PaymentResult processCardPayment(Long userId, Long orderId, long amount, PaymentMethod paymentMethod) {
        log.info("processCardPayment 시작 - orderId: {}, amount: {}", orderId, amount);
        try {
            // 1. Payment 엔티티 생성 (PENDING 상태)
            Payment payment = Payment.of(orderId, paymentMethod, amount);
            log.info("Payment 엔티티 생성 완료 - orderId: {}", orderId);
            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment 저장 완료 - paymentId: {}", savedPayment.getId());

            // 2. PG 결제 요청 (비동기)
            PaymentResult result = requestPgPayment(savedPayment, amount, paymentMethod);
            log.info("requestPgPayment 완료 - orderId: {}, success: {}", orderId, result.success());
            return result;
        } catch (Exception e) {
            log.error("processCardPayment 예외 발생 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            return PaymentResult.failure("카드 결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // PG 요청은 트랜잭션 외부에서 (또는 별도 트랜잭션)
    private PaymentResult requestPgPayment(Payment payment, long amount, PaymentMethod paymentMethod) {
        try {
            Long orderId = payment.getOrderId();
            log.info("PG 결제 요청 시작 - orderId: {}, amount: {}", orderId, amount);
            PgPaymentResponse pgResponse = pgPaymentService.processPayment(
                    orderId,
                    paymentMethod.getCardType().name(),
                    paymentMethod.getCardNo(),
                    amount
            );
            log.info("PG 결제 응답 받음 - orderId: {}, success: {}, status: {}", orderId, pgResponse.isSuccess(), pgResponse.getStatus());

            String transactionKey = pgResponse.getTransactionKey();
            
            if (pgResponse.isSuccess()) {
                log.info("카드 결제 요청 성공 - orderId: {}, transactionKey: {}", orderId, transactionKey);
                
                // Payment 엔티티에 transactionKey 업데이트
                updatePaymentTransactionKey(payment, transactionKey);
                
                return PaymentResult.cardRequestSuccess(transactionKey);
            } else {
                log.warn("카드 결제 요청 실패 - orderId: {}, status: {}", orderId, pgResponse.getStatus());
                
                // Fallback으로 인한 transactionKey가 있으면 업데이트
                if (transactionKey != null && transactionKey.startsWith("FALLBACK_")) {
                    log.info("Fallback transactionKey 업데이트 - orderId: {}, transactionKey: {}", orderId, transactionKey);
                    updatePaymentTransactionKey(payment, transactionKey);
                }
                
                return PaymentResult.failure("카드 결제 요청 실패: " + pgResponse.getStatus());
            }
        } catch (Exception e) {
            log.error("카드 결제 처리 중 예외 발생 - orderId: {}", payment.getOrderId(), e);
            return PaymentResult.failure("카드 결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

    @Transactional
    public void handlePaymentCallback(PgCallbackRequest callbackRequest) {
        log.info("결제 콜백 처리 시작 - transactionKey: {}", callbackRequest.transactionKey());

        // 1. 유효성 검증
        if (callbackRequest.transactionKey() == null || callbackRequest.transactionKey().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "transactionKey가 없습니다.");
        }

        // 2. Payment 조회
        Payment payment = paymentRepository.findByTransactionKey(callbackRequest.transactionKey())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다: " + callbackRequest.transactionKey()));

        // 3. 멱등성 검증 (이미 처리 완료된 경우 중복 처리 방지)
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("이미 처리된 결제 콜백 - transactionKey: {}, currentStatus: {}", callbackRequest.transactionKey(), payment.getStatus());
            return; // 중복 처리 방지
        }

        // 4. 결제 상태 업데이트
        if (callbackRequest.isSuccess()) {
            payment.markAsSuccess(callbackRequest.transactionKey());
            log.info("결제 성공 처리 - transactionKey: {}", callbackRequest.transactionKey());
        } else {
            payment.markAsFailed();
            log.warn("결제 실패 처리 - transactionKey: {}, reason: {}", callbackRequest.transactionKey(), callbackRequest.message());
        }

        // 5. Order 상태 연동
        updateRelatedOrderStatus(payment, callbackRequest.isSuccess());
    }

    private void updateRelatedOrderStatus(Payment payment, boolean paymentSuccess) {
        try {
            // Payment의 orderId로 Order 조회
            Order order = orderRepository.findByIdWithItems(payment.getOrderId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문 정보를 찾을 수 없습니다: " + payment.getOrderId()));

            if (paymentSuccess) {
                // 결제 성공 시 주문 완료
                log.info("주문 완료 처리 - orderId: {}, paymentId: {}", payment.getOrderId(), payment.getId());

                order.complete();
                orderRepository.save(order);
            } else {
                // 결제 실패 시 주문 취소
                log.warn("결제 실패로 인한 주문 취소 - orderId: {}, paymentId: {}", payment.getOrderId(), payment.getId());
                order.cancel("결제 실패");
                orderRepository.save(order);
            }

        } catch (Exception e) {
            log.error("주문 상태 업데이트 실패 - orderId: {}, error: {}", payment.getOrderId(), e.getMessage(), e);
        }
    }


    /**
     * PG에서 결제 상태 조회하여 우리 시스템과 비교
     */
    @Transactional(readOnly = true)
    public PaymentStatusInfo checkPaymentStatus(String transactionKey, String userId) {
        try {
            // 1. 우리 시스템의 Payment 조회
            Payment ourPayment = paymentRepository.findByTransactionKey(transactionKey)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                            "결제 정보를 찾을 수 없습니다: " + transactionKey));

            // 2. PG에서 최신 상태 조회
            PgPaymentResponse pgResponse = pgPaymentService.getPaymentInfo(userId, transactionKey);

            // 3. 상태 비교
            String ourStatus = ourPayment.getStatus().name();
            String pgStatus = pgResponse.getStatus();
            boolean isSync = isStatusSynchronized(ourStatus, pgStatus);

            log.info("결제 상태 확인 - transactionKey: {}, 우리시스템: {}, PG: {}, 동기화: {}",
                    transactionKey, ourStatus, pgStatus, isSync);

            return new PaymentStatusInfo(
                    transactionKey,
                    ourPayment.getOrderId(),
                    ourPayment.getId(),
                    ourStatus,
                    pgStatus,
                    isSync,
                    isSync ? "상태 일치" : "상태 불일치 - 수동 동기화 필요"
            );

        } catch (Exception e) {
            log.error("결제 상태 확인 실패 - transactionKey: {}, error: {}", transactionKey, e.getMessage(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 상태 확인 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * PG 상태와 강제 동기화
     */
    @Transactional
    public PaymentStatusInfo syncPaymentStatus(String transactionKey, String userId) {
        try {
            // 1. 우리 시스템의 Payment 조회
            Payment ourPayment = paymentRepository.findByTransactionKey(transactionKey)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                            "결제 정보를 찾을 수 없습니다: " + transactionKey));

            // 2. PG에서 최신 상태 조회
            PgPaymentResponse pgResponse = pgPaymentService.getPaymentInfo(userId, transactionKey);

            String ourStatus = ourPayment.getStatus().name();
            String pgStatus = pgResponse.getStatus();
            boolean wasSync = isStatusSynchronized(ourStatus, pgStatus);

            // 3. 불일치 시 동기화 수행
            if (!wasSync) {
                syncPaymentWithPgStatus(ourPayment, pgStatus);
                log.info("결제 상태 동기화 완료 - transactionKey: {}, {} → {}",
                        transactionKey, ourStatus, pgStatus);
            }

            return new PaymentStatusInfo(
                    transactionKey,
                    ourPayment.getOrderId(),
                    ourPayment.getId(),
                    ourPayment.getStatus().name(),
                    pgStatus,
                    true,
                    wasSync ? "이미 동기화됨" : "동기화 완료"
            );

        } catch (Exception e) {
            log.error("결제 상태 동기화 실패 - transactionKey: {}, error: {}", transactionKey, e.getMessage(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 상태 동기화 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 상태 동기화 여부 확인
     */
    private boolean isStatusSynchronized(String ourStatus, String pgStatus) {
        return switch (ourStatus) {
            case "PENDING" -> pgStatus.equals("PROCESSING") || pgStatus.equals("PENDING");
            case "SUCCESS" -> pgStatus.equals("SUCCESS");
            case "FAILED" -> pgStatus.equals("FAILED") || pgStatus.equals("CANCELLED");
            default -> false;
        };
    }

    /**
     * PG 상태에 맞춰 동기화
     */
    private void syncPaymentWithPgStatus(Payment payment, String pgStatus) {
        switch (pgStatus) {
            case "SUCCESS" -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.markAsSuccess(payment.getTransactionKey());
                    updateRelatedOrderStatus(payment, true);
                }
            }
            case "FAILED", "CANCELLED" -> {
                if (payment.getStatus() != PaymentStatus.FAILED) {
                    payment.markAsFailed();
                    log.warn("PG 결제 실패 확인 - 주문 상태는 수동 검토 필요");
                }
            }
        }
    }

    /**
     * Payment 엔티티에 transactionKey 업데이트 (별도 트랜잭션)
     */
    @Transactional
    private void updatePaymentTransactionKey(Payment payment, String transactionKey) {
        try {
            // Payment 엔티티를 다시 조회하여 영속화 상태로 만듦
            Payment managedPayment = paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다: " + payment.getId()));
            
            // transactionKey 업데이트
            managedPayment.updateTransactionKey(transactionKey);
            log.info("Payment transactionKey 업데이트 완료 - paymentId: {}, transactionKey: {}", payment.getId(), transactionKey);
            
        } catch (Exception e) {
            log.error("Payment transactionKey 업데이트 실패 - paymentId: {}, transactionKey: {}, error: {}", 
                    payment.getId(), transactionKey, e.getMessage(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "Payment transactionKey 업데이트 실패");
        }
    }

}
