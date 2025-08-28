package com.loopers.application.payment.event;

import com.loopers.domain.payment.PaymentType;

public record PaymentFailureEvent(
    Long orderId,
    Long userId,
    PaymentType paymentType,
    String transactionKey,
    Long amount,
    String failureReason,
    String processedAt
) {
    public static PaymentFailureEvent of(
        Long orderId, Long userId, PaymentType paymentType, String transactionKey,
        Long amount, String failureReason, String processedAt
    ) {
        return new PaymentFailureEvent(orderId, userId, paymentType, transactionKey, amount, failureReason, processedAt);
    }
}
