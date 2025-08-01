package com.loopers.application.order;

import com.loopers.domain.order.OrderItem;
import com.loopers.domain.product.Product;

public record OrderItemResponse(
        Long productId,
        String productName,
        int quantity,
        long price
) {
    public static OrderItemResponse of(OrderItem orderItem, Product product) {
        return new OrderItemResponse(
                orderItem.getProductId(),
                product != null ? product.getName() : "알 수 없는 상품", // 상품 정보가 없을 경우 대비
                orderItem.getQuantity(),
                orderItem.getPrice()
        );
    }
}
