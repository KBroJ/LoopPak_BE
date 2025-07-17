package com.loopers.domain.points;

import com.loopers.domain.users.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 *   단위 테스트
 *
 *   1. 포인트 충전
 *      - [X]  0 이하의 정수로 포인트를 충전 시 실패한다.
 */
@DisplayName("PointModel|단위 테스트")
@Nested
class PointModelTest {

    @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
    @Test
    void throwsBadRequestException_whenChargePointIsZeroOrLess() {

        // arrange
        UserModel userInfo = UserModel.of(
            "userId", "MALE", "2025-07-14", "test@test.com");
        Long chargePoint = 0L;

        // act
        CoreException result = assertThrows(CoreException.class, () -> {
            PointModel.of(userInfo, chargePoint);
        });

        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

}
