package com.loopers.interfaces.api;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *   E2E 테스트
 *   1. 회원가입
 *      - [ ]  회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.
 *      - [ ]  회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.
 *
 *   2. 내 정보 조회
 *      - [ ]  내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.
 *      - [ ]  존재하지 않는 ID 로 조회할 경우, `404 Not Found` 응답을 반환한다.
 *
 *   3. 포인트 조회
 *      - [ ]  포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.
 *      - [ ]  `X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.
 *
 *   4. 포인트 충전
 *      - [ ]  존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.
 *      - [ ]  존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {

    private static final Function<Long, String> ENDPOINT_GET = id -> "/api/v1/users/" + id;

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    private static final String USER_ID = "E2ETestId";
    private static final String GENDER = "MALE";
    private static final String BIRTH_DATE = "2025-07-15";
    private static final String EMAIL = "test@test.com";

    @Autowired
    public UserV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원가입")
    @Nested
    class User {

        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnCreatedUserInfo_whenSuccessSaveUser() {
            // arrange
            String requestUrl = "/api/v1/users";
            var requsest = new UsersV1Dto.UsersSaveRequest(
                USER_ID, GENDER, BIRTH_DATE, EMAIL
            );

            // act
            ParameterizedTypeReference<ApiResponse<UsersV1Dto.UsersResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UsersV1Dto.UsersResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(requsest), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().id()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(USER_ID),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(GENDER),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo(BIRTH_DATE),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(EMAIL)
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void throwsBadRequest_whenGenderIsNotExist() {
            // arrange
            String requestUrl = "/api/v1/users";
            var requsest = new UsersV1Dto.UsersSaveRequest(
                    USER_ID, null, BIRTH_DATE, EMAIL
            );

            // act
            ParameterizedTypeReference<ApiResponse<UsersV1Dto.UsersResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UsersV1Dto.UsersResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(requsest), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

    }

}
