package com.loopers.application.payment.processor;

import com.loopers.application.payment.PaymentResult;
import com.loopers.application.payment.PgPaymentService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgPaymentStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드 결제 처리 전략
 */
@Slf4j
@Component("CARD")   // ← Spring이 Map에 "CARD" 키로 등록
@RequiredArgsConstructor
public class CardPaymentProcessor implements PaymentProcessor {

    private final PgPaymentService pgPaymentService;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional  // 카드 결제는 PENDING 저장까지만
    public PaymentResult process(PaymentContext context) {
        log.info("카드 결제 처리 시작 - userId: {}, orderId: {}, amount: {}",
                context.getUserId(), context.getOrderId(), context.getAmount());

        try {
            // 1. Payment 엔티티 생성 (PENDING 상태)
            Payment payment = Payment.of(context.getOrderId(), context.getPaymentMethod(), context.getAmount());
            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment 저장 완료 - paymentId: {}", savedPayment.getId());

            // 2. PG 결제 요청
            PaymentResult result = requestPgPayment(context, savedPayment);
            log.info("PG 결제 요청 완료 - orderId: {}, success: {}", context.getOrderId(), result.success());

            // 3. PG 요청 실패 시 Payment 상태도 업데이트
            if (!result.success()) {

                // 명확한 실패(한도초과, 유효한 카드 아님 등)인 경우만 FAILED로 변경
                if (result.status() == PaymentStatus.FAILED) {
                    savedPayment.markAsFailed();
                    paymentRepository.save(savedPayment);
                }

            }

            return result;

        } catch (Exception e) {
            log.error("카드 결제 처리 중 예외 발생 - orderId: {}, error: {}", context.getOrderId(), e.getMessage(), e);
            return PaymentResult.failure("카드 결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // PG 요청은 트랜잭션 외부에서 (또는 별도 트랜잭션)
    private PaymentResult requestPgPayment(PaymentContext context, Payment payment) {
        try {
            PgPaymentResponse pgResponse = pgPaymentService.processPayment(
                    context.getUserId().toString(),
                    context.getOrderId(),
                    context.getPaymentMethod().getCardType().name(),
                    context.getPaymentMethod().getCardNo(),
                    context.getAmount()
            );

            String transactionKey = pgResponse.getTransactionKey();

            if (pgResponse.isSuccess()) {
                // 성공 처리 로직
                updatePaymentTransactionKey(payment, transactionKey);
                return PaymentResult.cardRequestSuccess(transactionKey);
            } else {
                // 실패 처리 로직 - 기존 handlePgFailure() 로직 활용
                return handlePgFailure(payment, pgResponse, transactionKey, context.getOrderId());
            }

        } catch (Exception e) {
            log.error("PG 결제 요청 중 예외 발생 - orderId: {}", context.getOrderId(), e);
            return PaymentResult.failure("PG 결제 요청 중 오류 발생: " + e.getMessage());
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
                if (transactionKey == null) {
                    // Fallback 상황: PG 요청 자체 실패, 재시도 필요
                    log.warn("PG 요청 실패로 인한 Fallback - orderId: {}, 재시도 필요", orderId);
                    // transactionKey null로 두고 PENDING 유지
                    return PaymentResult.fallback(null, "PG 시스템 장애 - 재시도 예정");
                } else {
                    // 진짜 실패: PG에서 받은 transactionKey 있음 (이력 확인용으로 저장)
                    log.warn("카드 결제 명확한 실패 - orderId: {}, transactionKey: {}", orderId, transactionKey);
                    updatePaymentTransactionKey(payment, transactionKey);
                    payment.markAsFailed();
                    paymentRepository.save(payment);
                    return PaymentResult.failure("카드 결제 실패: 유효하지 않은 카드이거나 한도가 초과되었습니다.");
                }
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
