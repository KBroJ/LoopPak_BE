package com.loopers.application.payment;

import com.loopers.application.payment.processor.PaymentContext;
import com.loopers.application.payment.processor.PaymentProcessor;
import com.loopers.domain.payment.*;
import com.loopers.interfaces.api.payment.PgCallbackRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final Map<String, PaymentProcessor> paymentProcessorMap;
    private final PaymentCallbackService paymentCallbackService;
    private final PaymentSyncService paymentSyncService;

    @Transactional
    public PaymentResult processPayment(
        Long userId, Long orderId, long amount,
        PaymentType paymentType, PaymentMethod paymentMethod
    ) {
        log.info("결제 처리 시작 - userId: {}, orderId: {}, amount: {}, paymentType: {}", userId, orderId, amount, paymentType);
        try {

            // 1. 결제 처리 전략 선택(전략패턴)
            PaymentProcessor processor = paymentProcessorMap.get(paymentType.name());
            if (processor == null) {
                throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 결제 방식입니다: " + paymentType);
            }

            // 2. PaymentContext 생성
            PaymentContext context = PaymentContext.of(userId, orderId, amount, paymentType, paymentMethod);

            // 3. 선택된 전략으로 결제 처리
            PaymentResult result = processor.process(context);

            log.info("결제 처리 완료 - userId: {}, orderId: {}, paymentType: {}, success: {}", userId, orderId, paymentType, result.success());

            return result;

        } catch (CoreException e) {
            log.error("CoreException 발생 - paymentType: {}, error: {}", paymentType, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("일반 Exception 발생 - paymentType: {}, error: {}", paymentType, e.getMessage(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 콜백 처리 위임
    @Transactional
    public void handlePaymentCallback(PgCallbackRequest callbackRequest) {
        paymentCallbackService.handlePaymentCallback(callbackRequest);
    }

    // 상태 동기화 위임
    @Transactional(readOnly = true)
    public PaymentStatusInfo checkPaymentStatus(String transactionKey, String userId) {
        return paymentSyncService.checkPaymentStatus(transactionKey, userId);
    }

    @Transactional
    public PaymentStatusInfo syncPaymentStatus(String transactionKey, String userId) {
        return paymentSyncService.syncPaymentStatus(transactionKey, userId);
    }

}
