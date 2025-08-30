package com.loopers.application.payment;

import com.loopers.infrastructure.pg.PgPaymentStatus;

public record PaymentStatusInfo(
        String transactionKey,
        Long orderId,
        Long paymentId,
        String ourStatus,           // "PENDING" | "SUCCESS" | "FAILED"
        PgPaymentStatus pgStatus,   // "SUCCESS" | "FAILED" | "CANCELLED"
        boolean isSync,
        String message
) {
}
