package com.loopers.domain.order;


import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long price; // 주문 당시의 상품 가격 (상품 가격이 변동될 수 있으므로)

    private OrderItem(Long productId, int quantity, long price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItem of(Long productId, int quantity, long price) {
        return new OrderItem (productId, quantity, price);
    }

    // Order 엔티티와의 연관관계를 설정하는 편의 메서드
    void setOrder(Order order) {
        this.order = order;
    }

    // 이 아이템의 총 가격을 계산하는 로직
    public long getTotalPrice() {
        return this.price * this.quantity;
    }

}
