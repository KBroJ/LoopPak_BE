package com.loopers.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {

        @DisplayName("주문 생성 시 주문 유저ID와 연결된 아이템 목록을 가진 Order를 생성한다.")
        @Test
        void createOrder_setsItemsAndInitialStatus() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.of(1L, 2, 10000),
                    OrderItem.of(2L, 1, 5000)
            );

            // act
            Order order = Order.of(userId, orderItems, 0L, OrderStatus.PAID);

            // assert
            assertAll(
                    () -> assertThat(order.getUserId()).isEqualTo(userId),
                    () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID),
                    () -> assertThat(order.getOrderItems()).hasSize(2),
                    () -> assertThat(order.getCouponId()).isNull() // 쿠폰 없는 주문
            );
        }

        @DisplayName("쿠폰과 함께 주문 생성 시 쿠폰 ID가 저장된다.")
        @Test
        void createOrder_withCoupon_savesCouponId() {
            // arrange
            Long userId = 1L;
            Long couponId = 100L;
            long discountAmount = 1000L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.of(1L, 1, 10000)
            );

            // act
            Order order = Order.of(userId, orderItems, discountAmount, couponId, OrderStatus.PENDING);

            // assert
            assertAll(
                    () -> assertThat(order.getUserId()).isEqualTo(userId),
                    () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                    () -> assertThat(order.getDiscountAmount()).isEqualTo(discountAmount),
                    () -> assertThat(order.getCouponId()).isEqualTo(couponId), // 쿠폰 ID 저장 확인
                    () -> assertThat(order.getOrderItems()).hasSize(1)
            );
        }
    }

    @DisplayName("총 주문 금액 계산")
    @Nested
    class CalculateTotalPrice {

        @DisplayName("여러 주문 아이템의 총 금액을 정확하게 계산한다.")
        @Test
        void calculateTotalPrice_withMultipleItems() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.of(1L, 2, 10000),
                    OrderItem.of(2L, 3, 5000)
            );
            Order order = Order.of(userId, orderItems, 0L, OrderStatus.PAID);

            // act
            long actualTotalPrice = order.calculateTotalPrice();

            // assert
            assertThat(actualTotalPrice).isEqualTo(35000L);
        }

        @DisplayName("주문 아이템이 하나일 경우 해당 아이템의 총 금액을 반환한다.")
        @Test
        void calculateTotalPrice_withSingleItem() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.of(101L, 5, 1000)
            );
            Order order = Order.of(userId, orderItems, 0L, OrderStatus.PAID);

            // act
            long actualTotalPrice = order.calculateTotalPrice();

            // assert
            assertThat(actualTotalPrice).isEqualTo(5000L);
        }

        @DisplayName("주문 아이템이 없을 경우 총 주문 금액은 0이다.")
        @Test
        void calculateTotalPrice_withNoItems() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of();
            Order order = Order.of(userId, orderItems, 0L, OrderStatus.PAID);

            // act
            long actualTotalPrice = order.calculateTotalPrice();

            // assert
            assertThat(actualTotalPrice).isZero();
        }
    }

}
