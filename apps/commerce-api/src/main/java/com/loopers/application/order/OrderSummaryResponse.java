package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;

import java.time.LocalDateTime;

// 주문 목록의 각 항목을 나타내는 DTO
public record OrderSummaryResponse(
        Long orderId,
        OrderStatus status,
        String representativeProductName, // 대표 상품명 (e.g., "상품 A 외 2건")
        long totalPrice,
        LocalDateTime orderDate
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                createRepresentativeProductName(order),
                order.calculateTotalPrice(),
                order.getCreatedAt().toLocalDateTime() // ZonedDateTime을 LocalDateTime으로 변환
        );
    }

    private static String createRepresentativeProductName(Order order) {
        if (order.getOrderItems().isEmpty()) {
            return "주문 상품 없음";
        }
        OrderItem firstItem = order.getOrderItems().get(0);
        // DB에서 상품 이름을 가져와야 하지만, 여기서는 ID로 간단히 표현
        String name = "상품 ID:" + firstItem.getProductId();

        if (order.getOrderItems().size() > 1) {
            return name + " 외 " + (order.getOrderItems().size() - 1) + "건";
        }
        return name;
    }

}
