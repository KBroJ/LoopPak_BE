package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponStatus;

import java.time.ZonedDateTime;

public record UserCouponInfo(
        Long userCouponId,
        UserCouponStatus status,
        ZonedDateTime expiresAt,
        CouponInfo couponInfo
) {
    public static UserCouponInfo of(UserCoupon userCoupon, Coupon coupon) {
        return new UserCouponInfo(
                userCoupon.getId(),
                userCoupon.getStatus(),
                userCoupon.getExpiresAt(),
                CouponInfo.from(coupon)
        );
    }
}
