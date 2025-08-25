package com.loopers.application.order;

public record OrderItemInfo(
    Long productId,
    int quantity
) {
}
