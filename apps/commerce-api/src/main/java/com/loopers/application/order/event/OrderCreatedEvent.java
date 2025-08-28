package com.loopers.application.order.event;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentType;

import java.time.Instant;

public record OrderCreatedEvent(
    Long orderId,                   // 생성된 주문 ID
    Long userId,                    // 주문한 사용자 ID
    Long couponId,                  // 사용할 쿠폰 ID (null 가능)
    long finalPrice,                // 최종 결제 금액
    PaymentType paymentType,        // 결제 방식
    PaymentMethod paymentMethod,    // 결제 수단 정보
    Instant occurredAt              // 이벤트 발생 시각
) {
    public static OrderCreatedEvent of(
            Long orderId, Long userId, Long couponId,
            long finalPrice, PaymentType paymentType,
            PaymentMethod paymentMethod
    ) {
        return new OrderCreatedEvent(
                orderId, userId, couponId, finalPrice,
                paymentType, paymentMethod, Instant.now()
        );
    }
}
