package com.loopers.application.order;

import java.util.List;

public record OrderInfo(
    List<OrderItemInfo> items,
    Long couponId,
    String paymentType,     // "POINT" or "CARD"
    CardInfo cardInfo       // 카드 결제시에만 필요
) {
}
