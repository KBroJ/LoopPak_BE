package com.loopers.application.payment.event;

import com.loopers.domain.payment.PaymentType;

public record PaymentSuccessEvent(
    Long orderId,
    Long userId,
    PaymentType paymentType,
    String transactionKey,
    Long amount,
    String message,
    String processedAt
) {
    public static PaymentSuccessEvent of(
        Long orderId, Long userId, PaymentType paymentType, String transactionKey,
        Long amount, String message, String processedAt
    ) {
        return new PaymentSuccessEvent(orderId, userId, paymentType, transactionKey, amount, message, processedAt);
    }
}
