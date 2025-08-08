package com.loopers.domain.coupon;

public interface DiscountPolicy {
    long calculateDiscount(long originalPrice, int discountValue);
}
