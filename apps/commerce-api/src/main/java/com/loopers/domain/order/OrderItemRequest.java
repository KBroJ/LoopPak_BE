package com.loopers.domain.order;

// 주문할 각 상품의 ID와 수량
public record OrderItemRequest(
        Long productId,
        int quantity
) {}
