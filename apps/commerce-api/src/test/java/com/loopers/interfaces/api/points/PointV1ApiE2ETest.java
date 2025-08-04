package com.loopers.interfaces.api.points;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.points.PointService;
import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserService;
import com.loopers.infrastructure.points.PointJpaRepository;
import com.loopers.infrastructure.users.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.users.UsersV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final UserService userService;
    private final PointService pointService;

    @Autowired
    public PointV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            UserService userService,
            PointService pointService
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.userService = userService;
        this.pointService = pointService;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회")
    @Nested
    class GetPointInfo {

        @DisplayName("ID 입력값으로 포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnPointInfo_whenGetPointInfoByUserId() {

            // arrange
            UserModel user = userService.saveUser(
                    "testUser", "MALE", "2025-07-15", "test@test.com"
            );

            pointService.chargePoint(user.getUserId(), 100L);

            String requestUrl = "/api/v1/points";
            var headers = new HttpHeaders();
            headers.set("X-USER-ID", "testUser");

            // act
            ParameterizedTypeReference<ApiResponse<PointsV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointsV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // assert
            assertThat(response.getBody().data().totalPoint()).isEqualTo(100L);

        }

        @DisplayName("`X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void throwsBadRequest_whenXUserIdHeaderIsMissing() {

            // arrange
            String requestUrl = "/api/v1/points";

            // act
            ParameterizedTypeReference<ApiResponse<UsersV1Dto.UsersResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UsersV1Dto.UsersResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null, null), responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        }

    }

    @DisplayName("포인트 충전")
    @Nested
    class ChargePoints {

        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnAllChargedPoints_whenChargePoints() {

            // arrange
            UserModel user = userService.saveUser(
                    "testUser", "MALE", "2025-07-15", "test@test.com"
            );

            String requestUrl = "/api/v1/points/charge";
            var headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());
            PointsV1Dto.PointRequest request = new  PointsV1Dto.PointRequest(
                    1000L
            );


            // act
            ParameterizedTypeReference<ApiResponse<PointsV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointsV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);


            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data().totalPoint()).isEqualTo(1000L);

        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void throwsNotFound_whenChargePointsForNonExistentUser() {

            // arrange
            String requestUrl = "/api/v1/points/charge";
            var headers = new HttpHeaders();
            headers.set("X-USER-ID", "noExistUsr");
            PointsV1Dto.PointRequest request = new  PointsV1Dto.PointRequest(
                    1000L
            );


            // act
            ParameterizedTypeReference<ApiResponse<PointsV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointsV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        }

    }

}
