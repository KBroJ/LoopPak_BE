package com.loopers.domain.users;

import com.loopers.domain.example.ExampleModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserModelTest {

    private static final String VALID_USER_ID = "validId";
    private static final String INVALID_USER_ID = "input_Invalid_userId";
    private static final String VALID_GENDER = "MALE";
    private static final String INVALID_GENDER = null;
    private static final String VALID_BIRTH_DATE = "2025-07-14";
    private static final String INVALID_BIRTH_DATE = "2025-7-4";
    private static final String VALID_EMAIL = "test@test.kr";
    private static final String INVALID_EMAIL = "imnotemailaddress";

    @DisplayName("UserModel|User 객체 생성 단위 테스트")
    @Nested
    class UserModelCreate {

        @Test
        @DisplayName("ID가 영문 및 숫자로 10자 이내가 아니면 User 객체 생성에 실패한다")
        void throwsBadRequestException_whenUserIdIsInvalid() {

            // arrange
            String userId = INVALID_USER_ID;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                UserModel.of(userId, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }

        @Test
        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, User 객체 생성에 실패한다")
        void throwsBadRequestException_whenEmailIsInvalid() {

            // arrange
            String email = INVALID_EMAIL;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                UserModel.of(VALID_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, email);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }

        @Test
        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, User 객체 생성에 실패한다")
        void throwsBadRequestException_whenBirthDateIsInvalid() {

            // arrange
            String birthDate = INVALID_BIRTH_DATE;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                UserModel.of(VALID_USER_ID, VALID_GENDER, birthDate, VALID_EMAIL);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }
    }

}
