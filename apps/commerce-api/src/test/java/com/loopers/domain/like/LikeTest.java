package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LikeTest {

    private final Long USER_ID = 1l;
    private final Long TARGET_ID = 2l;
    private final LikeType TYPE_PRODUCT = LikeType.PRODUCT;


    @DisplayName("좋아요 생성할 때")
    @Nested
    class Create {

        @DisplayName("userId, targetId, type이 모두 정상적으로 주어지면, 좋아요가 생성된다.")
        @Test
        void createLike_whenUserIdTargetIdTypeAreProvided() {

            // Act
            Like like = Like.of(USER_ID, TARGET_ID, TYPE_PRODUCT);

            // Assert
            Assertions.assertThat(like).isNotNull();
            Assertions.assertThat(like.getUserId()).isEqualTo(USER_ID);
            Assertions.assertThat(like.getTargetId()).isEqualTo(TARGET_ID);
            Assertions.assertThat(like.getType()).isEqualTo(TYPE_PRODUCT);

        }

        @DisplayName("userId가 null로 주어질 때 BAD_REQUEST 예외를 던진다.")
        @Test
        void throwsBadRequestException_whenUserIdIsNull() {

            // Arrange
            Long userId = null;

            // Act
            CoreException result = assertThrows(CoreException.class, () -> {
                Like.of(userId, TARGET_ID, TYPE_PRODUCT);
            });

            // Assert
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }

        @DisplayName("targetId가 null로 주어질 때 BAD_REQUEST 예외를 던진다.")
        @Test
        void throwsBadRequestException_whenTargetIdIsNull() {

            // Arrange
            Long targetId = null;

            // Act
            CoreException result = assertThrows(CoreException.class, () -> {
                Like.of(USER_ID, targetId, TYPE_PRODUCT);
            });

            // Assert
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }

        @DisplayName("targetId가 null일 때 BAD_REQUEST 예외를 던진다.")
        @Test
        void throwsBadRequestException_whenTypeIsNull() {

            // Arrange
            LikeType type = null;

            // Act
            CoreException result = assertThrows(CoreException.class, () -> {
                Like.of(USER_ID, TARGET_ID, type);
            });

            // Assert
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }

    }
}
