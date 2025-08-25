package com.loopers.application.payment;

public record PaymentStatusInfo(
    String transactionKey,
    Long orderId,
    Long paymentId,
    String ourStatus,        // "PENDING" | "SUCCESS" | "FAILED"
    String pgStatus,         // "SUCCESS" | "FAILED" | "CANCELLED"
    boolean isSync,
    String message
) {
}
