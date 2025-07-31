package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LikeUseCaseIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserModel user;
    private Product product;
    @BeforeEach
    void setUp() {
        user = userService.saveUser("userid", "MALE", "2025-07-14", "test@test.kr");
        product = productService.create(Product.of(
                1l, "상품명", "설명", 100, 10, 10, ProductStatus.ACTIVE
        ));
    }

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }

    @DisplayName("상품 좋아요 등록")
    @Nested
    class doLike {

        @DisplayName("모든 정보가 주어지고 등록된 좋아요가 없다면 좋아요 처리를 한다.")
        @Test
        void doLike_whenUserIdTargetIdLikeTypeAreProvided() {

            // arrange

            // act
            likeService.like(user.getId(), product.getId(), LikeType.PRODUCT);

            // assert
            Optional<Like> result = likeRepository.findByUserIdAndTargetIdAndType(user.getId(), product.getId(), LikeType.PRODUCT);

            assertAll(
                    () -> assertThat(result).isNotEmpty()
            );

        }

    }

    @DisplayName("상품 좋아요 취소")
    @Nested
    class doUnLike {

        @DisplayName("모든 정보가 주어지고 등록된 좋아요가 있다면 좋아요 취소 처리를 한다.")
        @Test
        void unLike_whenLikeExist() {

            // arrange
            likeService.like(user.getId(), product.getId(), LikeType.PRODUCT);

            // act
            likeService.unLike(user.getId(), product.getId(), LikeType.PRODUCT);

            // assert
            Optional<Like> result = likeRepository.findByUserIdAndTargetIdAndType(user.getId(), product.getId(), LikeType.PRODUCT);

            assertAll(
                    () -> assertThat(result).isEmpty()
            );

        }

    }

    @DisplayName("내가 좋아요 한 상품 목록 조회")
    @Nested
    class findMyLikeProduct {

        @DisplayName("모든 정보가 주어지고 등록된 좋아요가 있다면 좋아요 취소 처리를 한다.")
        @Test
        void Like() {

            // arrange
            likeService.like(user.getId(), product.getId(), LikeType.PRODUCT);
            likeService.like(user.getId(), 2l, LikeType.PRODUCT);
            likeService.like(user.getId(), 3l, LikeType.PRODUCT);

            // act

            // assert
            Optional<Like> result = likeRepository.findByUserIdAndTargetIdAndType(user.getId(), product.getId(), LikeType.PRODUCT);

            assertAll(
                    () -> assertThat(result).isEmpty()
            );

        }

    }

}
