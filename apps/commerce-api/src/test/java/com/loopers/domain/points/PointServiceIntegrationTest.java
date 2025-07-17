package com.loopers.domain.points;

import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserRepository;
import com.loopers.domain.users.UserService;
import com.loopers.infrastructure.points.PointJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 *   통합 테스트
 *
 *   1. 포인트 조회
 *      - [X]  해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.
 *      - [X]  해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.
 *
 *   2. 포인트 충전
 *      - [X]  존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.
 *
 */
@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private PointService pointService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointJpaRepository pointJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회")
    @Nested
    class GetPointInfo {

        @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void getPointInfo_whenUserExists() {

            // arrange
            UserModel user = userService.saveUser(
                "validId", "MALE", "2025-07-14", "test@test.com"
            );
            PointModel userPoint = PointModel.of(user, 100L);
            pointJpaRepository.save(userPoint);

            // act
            PointModel pointInfo = pointService.getPointInfo(user.getUserId());

            // assert
            assertAll(
                () -> assertThat(pointInfo).isNotNull(),
                () -> assertThat(userPoint.getPoint()).isEqualTo(pointInfo.getPoint())
            );

        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        @Test
        void returnNull_whenGetPointInfoForUserNotExists() {

            // arrange
            String userId = "noExistUsr";

            // act
            PointModel pointInfo = pointService.getPointInfo(userId);

            // assert
            assertThat(pointInfo).isNull();

        }

    }

    @DisplayName("포인트 충전")
    @Nested
    class ChargePoint {

        @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
        @Test
        void failchargePoint_whenUserNotExists() {

            // arrange
            String userId = "noExistUsr";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointService.chargePoint(userId, 100L);
            });

            System.out.println("예외 메시지: " + exception.getMessage());

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

        }

    }

}
