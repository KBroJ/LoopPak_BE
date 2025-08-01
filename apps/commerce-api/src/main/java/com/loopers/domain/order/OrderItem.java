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
    private long price;

    private OrderItem(Long productId, int quantity, long price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItem of(Long productId, int quantity, long price) {
        return new OrderItem (productId, quantity, price);
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public long getTotalPrice() {
        return this.price * this.quantity;
    }

}
