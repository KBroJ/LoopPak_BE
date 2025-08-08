package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UserCouponTest {

    @Test
    @DisplayName("사용자가 쿠폰 발급 시, 쿠폰은 '사용 가능(AVAILABLE)' 상태여야 한다.")
    void couponStatusIsAvailable_whenCreateUserCupon() {
        // arrange & act
        UserCoupon userCoupon = UserCoupon.of(1L, 1L, ZonedDateTime.now().plusDays(30));

        // assert
        assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
        assertThat(userCoupon.getUsedAt()).isNull();
    }

    @DisplayName("쿠폰 사용")
    @Nested
    class UseCoupon {

        @Test
        @DisplayName("사용 가능한 쿠폰을 정상적으로 사용 처리한다.")
        void useCouponSuccessfully_whenUseAvailableCoupon() {
            // arrange
            UserCoupon userCoupon = UserCoupon.of(1L, 1L, ZonedDateTime.now().plusDays(30));

            // act
            userCoupon.use();

            // assert
            assertThat(userCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
            assertThat(userCoupon.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 사용된 쿠폰을 다시 사용하려고 하면 예외가 발생한다.")
        void throwsBadRequestException_whenUsingUsedCoupon() {
            // arrange
            UserCoupon userCoupon = UserCoupon.of(1L, 1L, ZonedDateTime.now().plusDays(30));
            userCoupon.use();

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                userCoupon.use();
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("기간이 만료된 쿠폰을 사용하려고 하면 예외가 발생한다.")
        void throwsBadRequestException_whenUsingExpiredCoupon() {
            // arrange
            // 만료일을 어제로 설정하여 의도적으로 만료된 쿠폰을 생성
            UserCoupon expiredCoupon = UserCoupon.of(1L, 1L, ZonedDateTime.now().minusDays(1));

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                expiredCoupon.use();
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

}
