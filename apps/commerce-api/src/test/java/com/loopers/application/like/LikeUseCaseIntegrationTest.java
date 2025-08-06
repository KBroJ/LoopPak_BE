package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.users.User;
import com.loopers.domain.users.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeUseCaseIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;
    private Product product1;
    private Long brandAId;

    @BeforeEach
    void setUp() {
        user = userService.saveUser("userid", "MALE", "2025-07-14", "test@test.kr");
        Brand brandA = brandService.create(Brand.of("브랜드A", "설명", true));
        brandAId = brandA.getId();
        product1 = productService.create(Product.of(
                brandAId, "상품명1", "설명", 100, 10, 10, ProductStatus.ACTIVE
        ));
    }

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }

    @DisplayName("상품 좋아요")
    @Nested
    class doLike {
        @Test
        @DisplayName("좋아요가 등록된다.")
        void doLike_whenUserIdTargetIdLikeTypeAreProvided() {

            // act
            likeFacade.likeProduct(user.getId(), product1.getId());

            // assert
            Optional<Like> result = likeRepository.findByUserIdAndTargetIdAndType(user.getId(), product1.getId(), LikeType.PRODUCT);
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("등록된 좋아요가 있다면 좋아요가 취소된다.")
        void unLike_whenLikeExist() {

            // arrange
            likeFacade.likeProduct(user.getId(), product1.getId());

            // act
            likeFacade.unlikeProduct(user.getId(), product1.getId());

            // assert
            Optional<Like> result = likeRepository.findByUserIdAndTargetIdAndType(user.getId(), product1.getId(), LikeType.PRODUCT);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이미 좋아요를 누른 상품에 다시 요청해도 중복 저장되지 않는다.")
        void doesNotSaveDuplicate_whenLikeIsAlreadyExists() {
            // arrange.
            likeFacade.likeProduct(user.getId(), product1.getId());

            // act
            likeFacade.likeProduct(user.getId(), product1.getId());

            // assert
            List<Like> userLikes = likeRepository.findByUserIdAndType(user.getId(), LikeType.PRODUCT);
            assertThat(userLikes).hasSize(1);
        }

    }


    @DisplayName("내가 좋아요 한 상품 목록 조회")
    @Nested
    class findMyLikeProduct {
        @Test
        @DisplayName("내가 좋아요한 상품정보 목록을 조회한다.")
        void returnLikedProductsInfo_whenFindMyLikedProducts() {

            // arrange
            Product product2 = productService.create(Product.of(brandAId, "상품명2", "설명", 200, 10, 10, ProductStatus.ACTIVE));
            productService.create(Product.of(brandAId, "좋아요 안한 상품", "설명", 300, 10, 10, ProductStatus.ACTIVE));

            likeFacade.likeProduct(user.getId(), product1.getId());
            likeFacade.likeProduct(user.getId(), product2.getId());

            // act
            Page<Product> result = likeFacade.getLikedProducts(user.getId(), 0, 10);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting("id")
                    .containsExactlyInAnyOrder(product1.getId(), product2.getId());
        }

        @Test
        @DisplayName("좋아요 한 상품이 없을 경우, 빈 페이지가 반환된다.")
        void returnEmptyPage_whenUserHasNoLikes() {

            // arrange

            // act
            Page<Product> result = likeFacade.getLikedProducts(user.getId(), 0, 10);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
        }
    }

}
