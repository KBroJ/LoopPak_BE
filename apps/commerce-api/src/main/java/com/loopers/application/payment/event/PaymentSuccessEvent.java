package com.loopers.application.payment.event;

public record PaymentSuccessEvent(
    Long orderId,
    Long userId,
    String transactionKey,
    Long amount,
    String message,
    String processedAt
) {
    public static PaymentSuccessEvent of(
        Long orderId, Long userId, String transactionKey,
        Long amount, String message, String processedAt
    ) {
        return new PaymentSuccessEvent(orderId, userId, transactionKey, amount, message, processedAt);
    }
}
