package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private long discountAmount = 0L;

    @Column(name = "coupon_id")
    private Long couponId;  // 사용된 쿠폰 ID (복구 및 이력 추적용)

    private Order(Long userId, List<OrderItem> orderItems, long discountAmount, OrderStatus status) {
        this.userId = userId;
        this.discountAmount = discountAmount;
        this.status = status;
        orderItems.forEach(this::addOrderItem);
    }

    private Order(Long userId, List<OrderItem> orderItems, long discountAmount, Long couponId, OrderStatus status) {
        this.userId = userId;
        this.discountAmount = discountAmount;
        this.couponId = couponId;
        this.status = status;
        orderItems.forEach(this::addOrderItem);
    }

    public static Order of(Long userId, List<OrderItem> orderItems, long discountAmount, OrderStatus status) {
        return new Order(userId, orderItems, discountAmount, status);
    }

    public static Order of(Long userId, List<OrderItem> orderItems, long discountAmount, Long couponId, OrderStatus status) {
        return new Order(userId, orderItems, discountAmount, couponId, status);
    }

    private void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public long calculateTotalPrice() {
        return orderItems.stream()
                .mapToLong(OrderItem::getTotalPrice)
                .sum();
    }

    // 총 결제 금액 계산 시 할인 금액 차감
    public long getFinalPaymentPrice() {
        long originalTotalPrice = orderItems.stream()
                .mapToLong(OrderItem::getTotalPrice)
                .sum();
        return originalTotalPrice - discountAmount;
    }

    public void complete() {
        this.status = OrderStatus.PAID;
    }

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
    }

}
