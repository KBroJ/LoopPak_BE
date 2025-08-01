package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OrderDetailResponse(
        Long orderId,
        OrderStatus status,
        long totalPrice,
        LocalDateTime orderDate,
        List<OrderItemResponse> orderItems
) {
    public static OrderDetailResponse of(Order order, Map<Long, Product> productMap) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(orderItem -> OrderItemResponse.of(orderItem, productMap.get(orderItem.getProductId())))
                .toList();

        return new OrderDetailResponse(
                order.getId(),
                order.getStatus(),
                order.calculateTotalPrice(),
                order.getCreatedAt().toLocalDateTime(),
                itemResponses
        );
    }
}
