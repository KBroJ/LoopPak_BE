package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;

public record CouponInfo(
        Long id,
        String name,
        String description,
        CouponType type,
        int discountValue
) {
    public static CouponInfo from(Coupon coupon) {
        return new CouponInfo(
                coupon.getId(),
                coupon.getName(),
                coupon.getDescription(),
                coupon.getType(),
                coupon.getDiscountValue()
        );
    }
}
