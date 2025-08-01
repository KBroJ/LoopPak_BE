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

    private Order(Long userId, List<OrderItem> orderItems) {
        this.userId = userId;
        this.status = OrderStatus.PENDING; // 주문 생성 시 초기 상태는 PENDING
        orderItems.forEach(this::addOrderItem);
    }

    public static Order of(Long userId, List<OrderItem> orderItems) {
        return new Order(userId, orderItems);
    }

    // 연관관계 편의 메서드
    private void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    // 주문의 총 가격을 계산하는 핵심 도메인 로직
    public long calculateTotalPrice() {
        return orderItems.stream()
                .mapToLong(OrderItem::getTotalPrice)
                .sum();
    }

}
