package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgPaymentStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 동기화 전담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncService {

    private final PaymentRepository paymentRepository;
    private final PgPaymentService pgPaymentService;
    private final PaymentCallbackService paymentCallbackService;

    /**
     * PG에서 결제 상태 조회하여 우리 시스템과 비교
     */
    @Transactional(readOnly = true)
    public PaymentStatusInfo checkPaymentStatus(String transactionKey, String userId) {
        try {
            // 1. 우리 시스템의 Payment 조회
            Payment ourPayment = paymentRepository.findByTransactionKey(transactionKey)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다: " + transactionKey));

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

                    paymentCallbackService.updateRelatedOrderStatus(payment, true);
                    log.info("결제 성공으로 동기화 완료 - orderId: {} → PAID", payment.getOrderId());
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

}
