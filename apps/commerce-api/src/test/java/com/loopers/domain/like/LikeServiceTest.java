package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @InjectMocks
    private LikeService likeService;

    @Mock
    private LikeRepository likeRepository;

    @Nested
    @DisplayName("좋아요 등록 시")
    class LikeProduct {

        @Test
        @DisplayName("기존에 좋아요가 없으면 save를 호출한다.")
        void callsSave_whenLikeDoesNotExist() {
            // given
            Long userId = 1L;
            Long productId = 10L;

            given(likeRepository.findByUserIdAndTargetIdAndType(userId, productId, LikeType.PRODUCT))
                    .willReturn(Optional.empty());

            // when
            likeService.like(userId, productId, LikeType.PRODUCT);

            // then
            then(likeRepository).should(times(1)).save(any(Like.class));
        }

        @Test
        @DisplayName("기존에 좋아요가 있으면 save를 호출하지 않는다.")
        void doesNotCallSave_whenLikeAlreadyExists() {
            // given
            Long userId = 1L;
            Long productId = 10L;
            Like mockLike = Like.of(userId, productId, LikeType.PRODUCT);

            given(likeRepository.findByUserIdAndTargetIdAndType(userId, productId, LikeType.PRODUCT))
                    .willReturn(Optional.of(mockLike));

            // when
            likeService.like(userId, productId, LikeType.PRODUCT);

            // then
            then(likeRepository).should(never()).save(any(Like.class));
        }
    }
}
