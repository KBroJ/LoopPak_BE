package com.loopers.application.points;

import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.users.User;
import com.loopers.domain.users.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PointApplicationServiceTest {

    @Autowired
    private PointApplicationService pointApplicationService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointRepository pointRepository;

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
            User savedUser = userRepository.save(User.of("validId", "MALE", "2025-07-14", "test@test.com"));
            pointRepository.save(Point.of(savedUser, 1000L));

            // act
            PointInfo pointInfo = pointApplicationService.getPointInfo(savedUser.getUserId());

            // assert
            assertThat(pointInfo).isNotNull();
            assertThat(pointInfo.point()).isEqualTo(1000L);
        }

        @DisplayName("신규 사용자일 경우 포인트 조회 시 0 포인트를 반환한다")
        @Test
        void getPointInfo_whenUserIsNew() {
            // arrange
            User savedUser = userRepository.save(User.of("user2", "MALE", "2025-01-01", "test2@test.com"));

            // act
            PointInfo pointInfo = pointApplicationService.getPointInfo(savedUser.getUserId());

            // assert
            assertThat(pointInfo).isNotNull();
            assertThat(pointInfo.point()).isEqualTo(0L);
        }
    }

    @DisplayName("포인트 충전")
    @Nested
    class ChargePoint {

        @DisplayName("사용자의 포인트가 정상적으로 충전된다")
        @Test
        void chargePoint_success() {
            // arrange
            User savedUser = userRepository.save(User.of("user3", "MALE", "2025-01-01","test3@test.com"));
            pointRepository.save(Point.of(savedUser, 1000L));
            Long amountToCharge = 500L;

            // act
            PointInfo pointInfo = pointApplicationService.chargePoint(savedUser.getUserId(), amountToCharge);
            Point actualPoint = pointRepository.findByUserId(savedUser.getId()).orElseThrow();

            // assert
            assertThat(pointInfo.point()).isEqualTo(1500L);
            assertThat(actualPoint.getPoint()).isEqualTo(1500L);
        }

        @DisplayName("존재하지 않는 유저 ID로 충전을 시도하면 예외가 발생한다")
        @Test
        void failchargePoint_whenUserNotExists() {
            // arrange
            String nonExistUserId = "non-exist-user";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointApplicationService.chargePoint(nonExistUserId, 100L);
            });
            
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("포인트 충전 동시성 테스트")
    @Nested
    class ChargePointConcurrency {

        @DisplayName("여러 스레드가 동시에 포인트 충전을 요청해도 데이터 정합성이 유지된다")
        @Test
        void chargePoint_concurrency_success_with_pessimistic_lock() throws InterruptedException {
            // arrange
            // 1. 테스트용 사용자 및 초기 포인트 데이터 설정
            User savedUser = userRepository.save(User.of("User", "MALE", "2025-01-01", "con_test@test.com"));
            pointRepository.save(Point.of(savedUser, 0L));
            long initialPoints = 0L;
            long amountPerThread = 100L;

            // 2. 동시성 테스트 환경 설정
            int threadCount = 10; // 10개의 스레드가 동시에 요청
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount); // 스레드 풀 생성
            CountDownLatch latch = new CountDownLatch(threadCount); // 모든 스레드가 준비될 때까지 대기시키는 도구

            // act
            // 3. 각 스레드가 수행할 작업을 정의하고 실행 요청
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        // chargePoint 메소드는 내부적으로 트랜잭션을 가지므로 각 스레드는 별도의 트랜잭션에서 실행됩니다.
                        pointApplicationService.chargePoint(savedUser.getUserId(), amountPerThread);
                    } finally {
                        latch.countDown(); // 작업 완료를 Latch에 알림
                    }
                });
            }

            // 4. 모든 스레드가 작업을 완료할 때까지 대기
            latch.await();

            // assert
            // 5. 모든 스레드의 작업이 끝난 후, 최종 포인트가 기대값과 일치하는지 확인
            Point finalPoint = pointRepository.findByUserId(savedUser.getId()).orElseThrow();
            long expectedPoints = initialPoints + (amountPerThread * threadCount); // 0 + (100 * 10) = 1000

            assertThat(finalPoint.getPoint()).isEqualTo(expectedPoints);
        }
    }

}
