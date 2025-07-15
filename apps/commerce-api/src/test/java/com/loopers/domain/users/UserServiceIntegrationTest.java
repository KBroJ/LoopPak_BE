package com.loopers.domain.users;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *   통합 테스트
 *   1. 회원가입
 *      - [X]  회원 가입시 User 저장이 수행된다. ( spy 검증 )
 *      - [X]  이미 가입된 ID 로 회원가입 시도 시, 실패한다.
 *
 *   2. 내 정보 조회
 *      - [ ]  해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.
 *      - [ ]  해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.
 *
 *   3. 포인트 조회
 *      - [ ]  해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.
 *      - [ ]  해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.
 *
 *   4. 포인트 충전
 *      - [ ]  존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.
 *
 */
@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String VALID_USER_ID = "validId";
    private static final String DUPULICATE_USER_ID = "validId";
    private static final String VALID_GENDER = "MALE";
    private static final String VALID_BIRTH_DATE = "2025-07-14";
    private static final String VALID_EMAIL = "test@test.kr";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원가입, 내정보찾기 통합테스트")
    @Nested
    class User {

        @DisplayName("회원 가입시 User 저장이 수행된다. ( spy 검증 )")
        @Test
        void exeUserSave_whenUserSave() {

            // arrange
            var spyUserRepository = spy(userRepository);
            UserService userService = new UserService(spyUserRepository);

            // act
            userService.saveUser(
                VALID_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
            );

            // assert
            verify(spyUserRepository, times(1)).save(any(UserModel.class));

        }

        @DisplayName("이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
        @Test
        void failSaveUser_whenDuplicateUserId() {

            // arrange
            userService.saveUser(
                    VALID_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
            );

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                userService.saveUser(
                        DUPULICATE_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
                );
            });
            System.out.println("exception.getErrorType() = " + exception.getErrorType());


            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);

        }


    }

}
