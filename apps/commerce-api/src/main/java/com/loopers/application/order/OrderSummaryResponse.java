package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;

import java.time.LocalDateTime;

// 주문 목록의 각 항목을 나타내는 DTO
public record OrderSummaryResponse(
        Long orderId,
        OrderStatus status,
        String representativeProductName,
        long totalPrice,
        LocalDateTime orderDate
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                createRepresentativeProductName(order),
                order.calculateTotalPrice(),
                order.getCreatedAt().toLocalDateTime()
        );
    }

    private static String createRepresentativeProductName(Order order) {
        if (order.getOrderItems().isEmpty()) {
            return "주문 상품 없음";
        }
        OrderItem firstItem = order.getOrderItems().get(0);
        String name = "상품 ID:" + firstItem.getProductId();

        if (order.getOrderItems().size() > 1) {
            return name + " 외 " + (order.getOrderItems().size() - 1) + "건";
        }
        return name;
    }

}
