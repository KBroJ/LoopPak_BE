package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PgPaymentService {

    private final PgClient pgClient;
    private final PaymentRepository paymentRepository;

    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "fallbackPayment")
    @Retry(name = "pgRetry")
    public PgPaymentResponse processPayment(Long orderId, String cardType, String cardNo, long amount) {
        log.info("PG 결제 요청 시작 - orderId: {}, amount: {}", orderId, amount);

        // 2. PG 결제 요청
        PgPaymentRequest request = PgPaymentRequest.of(
                orderId, cardType, cardNo, amount,
                "http://localhost:8080/api/v1/payments/callback"
        );

        PgPaymentResponse response = pgClient.requestPayment("135135", request);
        log.info("PG 결제 요청 완료 - orderId: {}, success: {}", orderId, response.isSuccess());

        return response;  // 성공/실패 상관없이 response 그대로 반환

    }

    // Fallback 메서드 - PG 장애 시 실행
    public PgPaymentResponse fallbackPayment(Long orderId, String cardType, String cardNo, long amount, Exception ex) {
        log.error("PG 시스템 장애로 인한 Fallback 실행 - orderId: {}, error: {}", orderId, ex.getMessage());

        // Fallback 시 실패 응답 생성
        return new PgPaymentResponse(
                new PgPaymentResponse.Meta("FAILURE"),
                new PgPaymentResponse.Data("FALLBACK_" + orderId + "_" + System.currentTimeMillis(), "FAILED")
        );
    }

    // 결제 상태 조회 (재시도 + Circuit Breaker 적용)
    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "fallbackGetPaymentStatus")
    @Retry(name = "pgRetry")
    public Payment getPaymentStatus(String transactionKey) {
        log.info("PG 결제 상태 조회 - transactionKey: {}", transactionKey);

        PgPaymentResponse response = pgClient.getPayment("135135", transactionKey);

        // DB에서 Payment 조회 후 상태 업데이트
        Payment payment = paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + transactionKey));

        // PG 응답에 따른 상태 업데이트
        updatePaymentStatus(payment, response);

        return paymentRepository.save(payment);
    }

    // 결제 상태 조회 Fallback
    public Payment fallbackGetPaymentStatus(String transactionKey, Exception ex) {
        log.error("PG 상태 조회 장애로 인한 Fallback - transactionKey: {}, error: {}", transactionKey, ex.getMessage());

        // DB에서만 조회해서 반환 (PG 상태는 나중에 수동 확인)
        return paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + transactionKey));
    }

    private Payment createPendingPayment(Long orderId, String cardType, String cardNo, long amount) {
        PaymentMethod paymentMethod = PaymentMethod.of(CardType.valueOf(cardType), cardNo);
        return Payment.of(orderId, paymentMethod, amount);
    }

    private void updatePaymentStatus(Payment payment, PgPaymentResponse response) {
        if (response.isSuccess() && "SUCCESS".equals(response.getStatus())) {
            payment.markAsSuccess(response.getTransactionKey());
            log.info("결제 성공 처리 완료 - transactionKey: {}", response.getTransactionKey());
        } else if (response.isSuccess() && "FAILED".equals(response.getStatus())) {
            payment.markAsFailed();
            log.warn("결제 실패 처리 완료 - transactionKey: {}", response.getTransactionKey());
        }
        // PENDING 상태는 그대로 유지
    }

}
