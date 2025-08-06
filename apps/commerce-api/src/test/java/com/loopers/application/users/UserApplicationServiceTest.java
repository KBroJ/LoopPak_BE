package com.loopers.application.users;

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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UserApplicationServiceTest {

    @Autowired
    private UserApplicationService userApplicationService;

    @MockitoSpyBean
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

    @DisplayName("회원가입")
    @Nested
    class userSave {

        @DisplayName("회원 가입시 User 저장이 수행된다. ( spy 검증 )")
        @Test
        void exeUserSave_whenUserSave() {

            // arrange

            // act
            userApplicationService.saveUser(
                VALID_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
            );

            // assert
            verify(userRepository, times(1)).save(any(User.class));

        }

        @DisplayName("이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
        @Test
        void failSaveUser_whenDuplicateUserId() {

            // arrange
            userApplicationService.saveUser(
                    VALID_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
            );

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                userApplicationService.saveUser(
                        DUPULICATE_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
                );
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);

        }

    }

    @DisplayName("내 정보 조회")
    @Nested
    class findUserInfo {

        @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void findUserInfo_whenUserIdExists() {

            // arrange
            userApplicationService.saveUser(
                    VALID_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
            );

            // act
            UserInfo result = userApplicationService.getMyInfo(VALID_USER_ID);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.id()).isNotNull(),
                () -> assertThat(result.userId()).isEqualTo(VALID_USER_ID),
                () -> assertThat(result.gender()).isEqualTo(VALID_GENDER),
                () -> assertThat(result.birthDate()).isEqualTo(VALID_BIRTH_DATE),
                () -> assertThat(result.email()).isEqualTo(VALID_EMAIL)
            );

        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, NOT_FOUND 예외처리가 반환된다.")
        @Test
        void returnNotFoundError_whenUserIdNotExists() {

            // arrange
            String nonExistentUserId = "nonExistentId";

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                UserInfo userInfo = userApplicationService.getMyInfo(nonExistentUserId);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

        }

    }

}
