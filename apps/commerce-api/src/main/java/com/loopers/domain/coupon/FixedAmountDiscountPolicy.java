package com.loopers.domain.coupon;

public class FixedAmountDiscountPolicy implements DiscountPolicy {
    @Override
    public long calculateDiscount(long originalPrice, int discountValue) {
        // 주문 금액보다 할인액이 클 수 없다.
        return Math.min(originalPrice, discountValue);
    }
}
