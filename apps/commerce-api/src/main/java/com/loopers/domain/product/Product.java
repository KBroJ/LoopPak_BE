package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "product",
    indexes = {
        @Index(name = "idx_product_brand_status_price", columnList = "brandId, status, price")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Column(name = "brand_id", nullable = false)
    @NotNull
    private Long brandId;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private long price;

    @NotNull
    private int stock;

    private int maxOrderQuantity;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @NotNull
    private long likeCount = 0L; // 기본값을 0으로 설정

    @Version
    private Long version;

    private Product(
            Long brandId, String name, String description, long price, int stock, int maxOrderQuantity, ProductStatus status
    ) {

        if (brandId == null || brandId < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "brandId 는 null이거나, 0 미만일 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 이름은 비어있을 수 없습니다.");
        }
        if (price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.");
        }
        if (stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.");
        }
        if (maxOrderQuantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최대 주문 수량은 0 이상이어야 합니다.");
        }
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상태는 null일 수 없습니다.");
        }

        this.brandId = brandId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.maxOrderQuantity = maxOrderQuantity;
        this.status = status;
    }

    public static Product of(
            Long brandId, String name, String description, long price, int stock, int maxOrderQuantity, ProductStatus status
    ) {
        return new Product(brandId, name, description, price, stock, maxOrderQuantity, status);
    }

    public void increasePrice(double amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격 인상액은 0 이상이어야 합니다.");
        }
        this.price += amount;
    }
    public void decreasePrice(double amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격 인하액은 0 이상이어야 합니다.");
        }
        if (this.price < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격이 부족합니다.");
        }
        this.price -= amount;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
        }
        this.stock += quantity;
    }
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
        }
        if (this.stock < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.stock -= quantity;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }
    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }
    public void outOfStock() {
        this.status = ProductStatus.OUT_OF_STOCK;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

}
