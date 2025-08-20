package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.infrastructure.pg.PgPaymentResponse;
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

    private final PaymentRepository paymentRepository;
    private final PointRepository pointRepository;
    private final PgPaymentService pgPaymentService;

    public PaymentResult processPayment(
        Long userId, Long orderId, long amount,
        PaymentType paymentType, PaymentMethod paymentMethod
    ) {
        try {
            switch (paymentType) {
                case POINT -> {
                    return processPointPayment(userId, amount);
                }
                case CARD -> {
                    return processCardPayment(userId, orderId, amount, paymentMethod);
                }
                default -> throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 결제 방식입니다.");
            }
        } catch (CoreException e) {
            return PaymentResult.failure(e.getMessage());
        } catch (Exception e) {
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
        // 1. Payment 엔티티 생성 (PENDING 상태)
        Payment payment = Payment.of(orderId, paymentMethod, amount);
        Payment savedPayment = paymentRepository.save(payment);

        // 2. PG 결제 요청 (비동기)
        return requestPgPayment(savedPayment.getId(), amount, paymentMethod);
    }

    // PG 요청은 트랜잭션 외부에서 (또는 별도 트랜잭션)
    private PaymentResult requestPgPayment(Long paymentId, long amount, PaymentMethod paymentMethod) {
        try {
            PgPaymentResponse pgResponse = pgPaymentService.processPayment(
                    paymentId,
                    paymentMethod.getCardType().name(),
                    paymentMethod.getCardNo(),
                    amount
            );

            if (pgResponse.isSuccess()) {
                String transactionKey = pgResponse.getTransactionKey();
                log.info("카드 결제 요청 성공 - paymentId: {}, transactionKey: {}", paymentId, transactionKey);
                return PaymentResult.cardRequestSuccess(transactionKey);
            } else {
                log.warn("카드 결제 요청 실패 - paymentId: {}, status: {}", paymentId, pgResponse.getStatus());
                return PaymentResult.failure("카드 결제 요청 실패: " + pgResponse.getStatus());
            }
        } catch (Exception e) {
            log.error("카드 결제 처리 중 예외 발생 - paymentId: {}", paymentId, e);
            return PaymentResult.failure("카드 결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

}
