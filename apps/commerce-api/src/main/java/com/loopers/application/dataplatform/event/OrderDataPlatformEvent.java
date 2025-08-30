package com.loopers.application.dataplatform.event;

import java.time.LocalDateTime;

public record OrderDataPlatformEvent(
        Long orderId,
        Long userId,
        Long totalAmount,
        Long discountAmount,
        String orderStatus,
        LocalDateTime completedAt
) {
    public static OrderDataPlatformEvent of(Long orderId, Long userId, Long totalAmount, Long discountAmount, String orderStatus) {
        return new OrderDataPlatformEvent(
                orderId,
                userId,
                totalAmount,
                discountAmount,
                orderStatus,
                LocalDateTime.now()
        );
    }
}