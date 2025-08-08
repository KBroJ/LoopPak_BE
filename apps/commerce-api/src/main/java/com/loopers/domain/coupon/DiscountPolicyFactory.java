package com.loopers.domain.coupon;

public class DiscountPolicyFactory {
    public static DiscountPolicy getPolicy(CouponType type) {
        return switch (type) {
            case FIXED -> new FixedAmountDiscountPolicy();
            case PERCENTAGE -> new PercentageDiscountPolicy();
            default -> throw new IllegalArgumentException("지원하지 않는 쿠폰 타입입니다.");
        };
    }
}
