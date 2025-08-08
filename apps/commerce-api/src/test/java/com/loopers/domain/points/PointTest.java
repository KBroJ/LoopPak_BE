package com.loopers.domain.points;

import com.loopers.domain.users.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PointModel|단위 테스트")
@Nested
class PointTest {

    @DisplayName("0 이하의 정수로 포인트를 충전 시 실패한다.")
    @Test
    void throwsBadRequestException_whenChargePointIsZeroOrLess() {

        // arrange
        User userInfo = User.of(
            "userId", "MALE", "2025-07-14", "test@test.com");
        Point point = Point.of(userInfo, 0L);

        Long chargePoint = 0L;

        // act
        CoreException result = assertThrows(CoreException.class, () -> {
            point.charge(chargePoint);
        });


        // assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

}
