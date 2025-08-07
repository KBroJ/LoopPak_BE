package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Getter
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @Column(nullable = false)
    private int discountValue; // 1000(원) 또는 10(%) 등 숫자 값

    @Column(nullable = false)
    private int totalQuantity; // 0이면 무제한

    @Column(nullable = false)
    private ZonedDateTime validFrom; // 쿠폰 발급 가능 시작일

    @Column(nullable = false)
    private ZonedDateTime validUntil; // 쿠폰 발급 가능 종료일

    // 생성자 및 of() 메소드
    private Coupon(String name, String description, CouponType type, int discountValue, int totalQuantity, ZonedDateTime validFrom, ZonedDateTime validUntil) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.discountValue = discountValue;
        this.totalQuantity = totalQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public static Coupon of(String name, String description, CouponType type, int discountValue, int totalQuantity, ZonedDateTime validFrom, ZonedDateTime validUntil) {
        return new Coupon(name, description, type, discountValue, totalQuantity, validFrom, validUntil);
    }

}
