package com.loopers.application.payment.event;

public record PaymentFailureEvent(
    Long orderId,
    Long userId,
    String transactionKey,
    Long amount,
    String failureReason,
    String processedAt
) {
    public static PaymentFailureEvent of(
        Long orderId, Long userId, String transactionKey,
        Long amount, String failureReason, String processedAt
    ) {
        return new PaymentFailureEvent(orderId, userId, transactionKey, amount, failureReason, processedAt);
    }
}
