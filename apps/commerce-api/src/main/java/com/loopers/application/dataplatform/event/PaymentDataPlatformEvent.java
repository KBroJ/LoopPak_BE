package com.loopers.application.dataplatform.event;

import java.time.LocalDateTime;

public record PaymentDataPlatformEvent(
        Long orderId,
        Long userId,
        String paymentType,
        Long amount,
        String paymentStatus,
        String transactionKey,
        LocalDateTime processedAt
) {
    public static PaymentDataPlatformEvent of(Long orderId, Long userId, String paymentType, Long amount, String paymentStatus, String transactionKey) {
        return new PaymentDataPlatformEvent(
                orderId,
                userId,
                paymentType,
                amount,
                paymentStatus,
                transactionKey,
                LocalDateTime.now()
        );
    }
}