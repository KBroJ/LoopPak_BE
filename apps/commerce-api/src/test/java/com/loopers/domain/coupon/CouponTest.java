package com.loopers.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CouponTest {

    @Test
    @DisplayName("쿠폰 정책(템플릿) 생성 시 Coupon.of()를 통해 정상적으로 쿠폰 객체가 생성된다.")
    void create_coupon_template_successfully() {
        // arrange
        String name = "여름맞이 10% 할인";
        CouponType type = CouponType.PERCENTAGE;
        int discountValue = 10;
        ZonedDateTime validFrom = ZonedDateTime.now();
        ZonedDateTime validUntil = ZonedDateTime.now().plusMonths(1);

        // act
        Coupon coupon = Coupon.of(name, "설명", type, discountValue, 1000, validFrom, validUntil);

        // assert
        assertThat(coupon.getName()).isEqualTo(name);
        assertThat(coupon.getType()).isEqualTo(type);
        assertThat(coupon.getDiscountValue()).isEqualTo(discountValue);
        assertThat(coupon.getValidFrom()).isEqualTo(validFrom);
        assertThat(coupon.getValidUntil()).isEqualTo(validUntil);
    }

}
