package com.loopers.interfaces.api.points;

import com.loopers.application.points.PointApplicationService;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
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
    private final UserApplicationService userApplicationService;
    private final PointApplicationService pointApplicationService;

    @Autowired
    public PointV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            UserApplicationService userApplicationService,
            PointApplicationService pointApplicationService
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.userApplicationService = userApplicationService;
        this.pointApplicationService = pointApplicationService;
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
            UserInfo user = userApplicationService.saveUser(
                    "testUser", "MALE", "2025-07-15", "test@test.com"
            );

            pointApplicationService.chargePoint(user.userId(), 100L);

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
            UserInfo user = userApplicationService.saveUser(
                    "testUser", "MALE", "2025-07-15", "test@test.com"
            );

            String requestUrl = "/api/v1/points/charge";
            var headers = new HttpHeaders();
            headers.set("X-USER-ID", user.userId());
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
