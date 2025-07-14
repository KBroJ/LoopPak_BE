package com.loopers.domain.users;

import com.loopers.utils.DatabaseCleanUp;
import com.ninjasquad.springmockk.SpykBean;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor
class UserServiceIntegrationTest {

    private UserService userService;
    @SpykBean
    private UserRepository userRepository;

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


            // act
            UserModel userModel = userService.saveUser(
                    VALID_USER_ID, VALID_GENDER, VALID_BIRTH_DATE, VALID_EMAIL
            );

            // assert




        }

    }

}
