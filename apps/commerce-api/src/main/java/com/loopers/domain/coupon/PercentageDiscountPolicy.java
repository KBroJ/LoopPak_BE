package com.loopers.domain.coupon;

public class PercentageDiscountPolicy implements DiscountPolicy {
    @Override
    public long calculateDiscount(long originalPrice, int discountValue) {
        return originalPrice * discountValue / 100;
    }
}
