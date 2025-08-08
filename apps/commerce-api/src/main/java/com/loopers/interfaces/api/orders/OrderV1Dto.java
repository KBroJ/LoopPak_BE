package com.loopers.interfaces.api.orders;

import com.loopers.application.order.OrderDetailResponse;
import com.loopers.application.order.OrderItemResponse;
import com.loopers.application.order.OrderSummaryResponse;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrderV1Dto {

    public record OrderRequest(List<OrderItemRequest> items, Long couponId) {}
    public record OrderItemRequest(Long productId, int quantity) {}


    public record OrderResponse(Long orderId) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(order.getId());
        }
    }

    public record OrderSummary(
            Long orderId,
            OrderStatus status,
            String representativeProductName,
            long totalPrice,
            LocalDateTime orderDate
    ) {
        public static OrderSummary from(OrderSummaryResponse response) {
            return new OrderSummary(
                    response.orderId(),
                    response.status(),
                    response.representativeProductName(),
                    response.totalPrice(),
                    response.orderDate()
            );
        }
    }

    public record OrderDetail(
            Long orderId,
            OrderStatus status,
            long totalPrice,
            LocalDateTime orderDate,
            List<OrderItemDetail> orderItems
    ) {
        public static OrderDetail from(OrderDetailResponse response) {
            List<OrderItemDetail> itemDetails = response.orderItems().stream()
                    .map(OrderItemDetail::from)
                    .toList();

            return new OrderDetail(
                    response.orderId(),
                    response.status(),
                    response.totalPrice(),
                    response.orderDate(),
                    itemDetails
            );
        }
    }

    public record OrderItemDetail(
            Long productId,
            String productName,
            int quantity,
            long price
    ) {
        public static OrderItemDetail from(OrderItemResponse response) {
            return new OrderItemDetail(
                    response.productId(),
                    response.productName(),
                    response.quantity(),
                    response.price()
            );
        }
    }

}
