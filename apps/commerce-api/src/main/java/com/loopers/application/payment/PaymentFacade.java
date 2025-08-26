package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.*;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgPaymentStatus;
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
    private final UserCouponRepository userCouponRepository;
    private final ProductRepository productRepository;

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
            PaymentResult result = requestPgPayment(userId, savedPayment, amount, paymentMethod);
            log.info("requestPgPayment 완료 - orderId: {}, success: {}", orderId, result.success());

            // PG 요청 실패 시 Payment 상태도 FAILED로 변경
            if (!result.success()) {
                savedPayment.markAsFailed();
                paymentRepository.save(savedPayment);
            }

            return result;

        } catch (Exception e) {
            log.error("processCardPayment 예외 발생 - orderId: {}, error: {}", orderId, e.getMessage(), e);

            // Payment가 저장되었는지 확인하여 다르게 처리
            paymentRepository.findByOrderId(orderId)
                    .ifPresentOrElse(
                            payment -> {
                                log.warn("Payment 저장 후 예외 발생 - PENDING 유지");
                            },
                            () -> {
                                log.error("Payment 저장 전 예외 발생");
                            }
                    );

            return PaymentResult.failure("카드 결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // PG 요청은 트랜잭션 외부에서 (또는 별도 트랜잭션)
    private PaymentResult requestPgPayment(Long userId, Payment payment, long amount, PaymentMethod paymentMethod) {
        try {
            Long orderId = payment.getOrderId();
            log.info("PG 결제 요청 시작 - userId: {}, orderId: {}, amount: {}", userId, orderId, amount);
            PgPaymentResponse pgResponse = pgPaymentService.processPayment(
                    userId.toString(),
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
                
                return handlePgFailure(payment, pgResponse, transactionKey, orderId);
            }
        } catch (Exception e) {
            log.error("카드 결제 처리 중 예외 발생 - orderId: {}", payment.getOrderId(), e);
            return PaymentResult.failure("카드 결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * PG 실패 응답을 상태별로 처리
     */
    private PaymentResult handlePgFailure(
            Payment payment, PgPaymentResponse pgResponse,
            String transactionKey, Long orderId
    ) {

        PgPaymentStatus status = pgResponse.getStatus();

        switch (status) {
            case FAILED -> {
                // 명확한 실패: 카드 한도 초과, 유효하지 않은 카드 등
                log.warn("카드 결제 명확한 실패 - orderId: {}, status: {}", orderId, status);
                payment.markAsFailed();
                paymentRepository.save(payment);
                return PaymentResult.failure("카드 결제 실패: 유효하지 않은 카드이거나 한도가 초과되었습니다.");
            }
            case CANCELLED -> {
                // 명확한 실패: 결제 취소
                log.warn("카드 결제 취소됨 - orderId: {}, status: {}", orderId, status);
                payment.markAsFailed();
                paymentRepository.save(payment);
                return PaymentResult.failure("카드 결제가 취소되었습니다.");
            }
            case PENDING, PROCESSING -> {
                // 불확실한 상태: 서버 장애, 타임아웃 등 - Payment는 PENDING 유지
                log.warn("카드 결제 처리중 또는 대기 - orderId: {}, status: {} - PENDING 유지", orderId, status);

                // Fallback transactionKey가 있으면 저장
                if (transactionKey != null && transactionKey.startsWith("FALLBACK_")) {
                    updatePaymentTransactionKey(payment, transactionKey);
                    return PaymentResult.fallback(transactionKey, "PG 시스템 일시 장애로 인한 Fallback 처리");
                }

                return PaymentResult.failure("PG 시스템 일시 장애 - 잠시 후 다시 시도해주세요.");
            }
            default -> {
                // 알 수 없는 상태 - 안전하게 PENDING 유지
                log.error("알 수 없는 PG 응답 상태 - orderId: {}, status: {} - PENDING 유지", orderId, status);
                return PaymentResult.failure("결제 처리 중 알 수 없는 오류가 발생했습니다.");
            }
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
                // 결제 실패 시 주문 취소 및 리소스 복구
                log.warn("결제 실패로 인한 주문 취소 및 복구 처리 - orderId: {}, paymentId: {}", payment.getOrderId(), payment.getId());
                order.cancel("결제 실패");
                orderRepository.save(order);
                
                // 재고 및 쿠폰 복구
                restoreOrderResources(order);
            }

        } catch (Exception e) {
            log.error("주문 상태 업데이트 실패 - orderId: {}, error: {}", payment.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 주문 실패 시 재고 및 쿠폰 복구
     */
    private void restoreOrderResources(Order order) {
        try {
            log.info("주문 리소스 복구 시작 - orderId: {}", order.getId());
            
            // 1. 재고 복구
            restoreProductStock(order);
            
            // 2. 쿠폰 복구 (사용된 쿠폰이 있는 경우)
            if (order.getCouponId() != null) {
                restoreCoupon(order.getCouponId());
            }
            
            log.info("주문 리소스 복구 완료 - orderId: {}", order.getId());
            
        } catch (Exception e) {
            log.error("주문 리소스 복구 실패 - orderId: {}, error: {}", order.getId(), e.getMessage(), e);
            // 복구 실패는 로그만 남기고 예외를 다시 던지지 않음 (결제 처리는 계속 진행)
        }
    }

    /**
     * 상품 재고 복구
     */
    private void restoreProductStock(Order order) {
        log.info("재고 복구 시작 - orderId: {}, 상품 수: {}", order.getId(), order.getOrderItems().size());
        
        order.getOrderItems().forEach(orderItem -> {
            try {
                Product product = productRepository.productInfo(orderItem.getProductId())
                        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, 
                                "상품을 찾을 수 없습니다: " + orderItem.getProductId()));
                
                // 재고 복구 (주문했던 수량만큼 다시 증가)
                product.increaseStock(orderItem.getQuantity());
                productRepository.save(product);
                
                log.info("재고 복구 완료 - productId: {}, quantity: {}, 복구 후 재고: {}", 
                        orderItem.getProductId(), orderItem.getQuantity(), product.getStock());
                        
            } catch (Exception e) {
                log.error("재고 복구 실패 - productId: {}, quantity: {}, error: {}", 
                        orderItem.getProductId(), orderItem.getQuantity(), e.getMessage(), e);
            }
        });
    }

    /**
     * 쿠폰 복구
     */
    private void restoreCoupon(Long couponId) {
        try {
            UserCoupon userCoupon = userCouponRepository.findById(couponId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: " + couponId));
            
            // 쿠폰 복구 (USED → AVAILABLE)
            userCoupon.restore();
            userCouponRepository.save(userCoupon);
            
            log.info("쿠폰 복구 완료 - couponId: {}, 복구 후 상태: {}", couponId, userCoupon.getStatus());
            
        } catch (Exception e) {
            log.error("쿠폰 복구 실패 - couponId: {}, error: {}", couponId, e.getMessage(), e);
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
            PgPaymentStatus pgStatus = pgResponse.getStatus();
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
            PgPaymentStatus pgStatus = pgResponse.getStatus();
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
    private boolean isStatusSynchronized(String ourStatus, PgPaymentStatus pgStatus) {
        return switch (ourStatus) {
            case "PENDING" -> pgStatus == PgPaymentStatus.PROCESSING || pgStatus == PgPaymentStatus.PENDING;
            case "SUCCESS" -> pgStatus == PgPaymentStatus.SUCCESS;
            case "FAILED" -> pgStatus == PgPaymentStatus.FAILED || pgStatus == PgPaymentStatus.CANCELLED;
            default -> false;
        };
    }

    /**
     * PG 상태에 맞춰 동기화
     */
    private void syncPaymentWithPgStatus(Payment payment, PgPaymentStatus pgStatus) {
        switch (pgStatus) {
            case SUCCESS -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.markAsSuccess(payment.getTransactionKey());
                    updateRelatedOrderStatus(payment, true);
                }
            }
            case FAILED, CANCELLED -> {
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
