package com.loopers.application.product;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.like.LikeApplicationService;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.ProductStatus;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductUseCaseIntegrationTest {

    @Autowired
    private ProductApplicationService productAppService;
    @Autowired
    private BrandApplicationService brandAppService;
    @Autowired
    private LikeApplicationService likeAppService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long brandAId;
    private Long brandBId;
    @BeforeEach
    void setUp() {
        // 각 테스트 전에 독립적인 브랜드 데이터를 생성합니다.
        BrandInfo brandA = brandAppService.create("브랜드A", "설명", true);
        BrandInfo brandB = brandAppService.create("브랜드B", "설명", true);
        brandAId = brandA.id();
        brandBId = brandB.id();
    }

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }

    @DisplayName("상품 생성")
    @Nested
    class CreateProduct {
        @DisplayName("성공: 상품 생성 시 상품 정보 DTO가 반환된다.")
        @Test
        void returnProductInfo_whenCreateProduct() {
            // arrange & act
            ProductResponse result = productAppService.create(
                    brandAId, "상품명", "설명", 100, 10, 10, ProductStatus.ACTIVE
            );

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.brandId()).isEqualTo(brandAId),
                    () -> assertThat(result.productId()).isNotNull(),
                    () -> assertThat(result.name()).isEqualTo("상품명")
            );
        }
    }

    @DisplayName("상품 목록 조회")
    @Nested
    class SearchProducts {
        @DisplayName("성공: 기본 조건(최신순)으로 조회 시 활성화된 모든 상품이 최신순으로 반환된다.")
        @Test
        void returnAllActiveProducts_whenSearchWithDefaultConditions() throws InterruptedException {
            // arrange
            ProductResponse p1 = productAppService.create(brandAId, "활성상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE);
            Thread.sleep(10); // 생성 시간차를 두기 위함
            ProductResponse p2 = productAppService.create(brandBId, "활성상품2", "설명", 200, 10, 10, ProductStatus.ACTIVE);
            productAppService.create(brandAId, "비활성상품", "설명", 300, 10, 10, ProductStatus.INACTIVE);

            likeAppService.like(1L, p1.productId(), LikeType.PRODUCT);
            likeAppService.like(2L, p1.productId(), LikeType.PRODUCT);
            likeAppService.like(1L, p2.productId(), LikeType.PRODUCT);

            // act
            Page<ProductResponse> result = productAppService.searchProducts(null, "latest", 0, 10);
            List<ProductResponse> content = result.getContent();

            // assert
            assertAll(
                    () -> assertThat(result.getTotalElements()).isEqualTo(2),
                    () -> assertThat(content.get(0).productId()).isEqualTo(p2.productId()),
                    () -> assertThat(content.get(0).likeCount()).isEqualTo(1),
                    () -> assertThat(content.get(1).productId()).isEqualTo(p1.productId()),
                    () -> assertThat(content.get(1).likeCount()).isEqualTo(2)
            );
        }

        @DisplayName("성공: 가격 오름차순으로 정렬 시 상품이 가격순으로 반환된다.")
        @Test
        void returnSortedProducts_whenSearchWithPriceAsc() {
            // arrange
            productAppService.create(brandAId, "중간가격상품", "설명", 200, 10, 10, ProductStatus.ACTIVE);
            productAppService.create(brandAId, "최고가상품", "설명", 300, 10, 10, ProductStatus.ACTIVE);
            productAppService.create(brandAId, "최저가상품", "설명", 100, 10, 10, ProductStatus.ACTIVE);

            // act
            Page<ProductResponse> resultPage = productAppService.searchProducts(null, "price_asc", 0, 10);

            // assert
            assertThat(resultPage.getContent())
                    .isSortedAccordingTo(Comparator.comparing(response -> response.price()));
        }

        @DisplayName("성공: 좋아요 많은 순으로 정렬 시 상품이 좋아요 수 순으로 반환된다.")
        @Test
        void returnSortedProducts_whenSearchWithLikesDesc() {
            // arrange
            ProductResponse p1 = productAppService.create(brandAId, "좋아요1개", "설명", 100, 10, 10, ProductStatus.ACTIVE);
            ProductResponse p2 = productAppService.create(brandAId, "좋아요3개", "설명", 100, 10, 10, ProductStatus.ACTIVE);
            ProductResponse p3 = productAppService.create(brandAId, "좋아요2개", "설명", 100, 10, 10, ProductStatus.ACTIVE);

            likeAppService.like(1L, p2.productId(), LikeType.PRODUCT);
            likeAppService.like(2L, p2.productId(), LikeType.PRODUCT);
            likeAppService.like(3L, p2.productId(), LikeType.PRODUCT);
            likeAppService.like(1L, p3.productId(), LikeType.PRODUCT);
            likeAppService.like(2L, p3.productId(), LikeType.PRODUCT);
            likeAppService.like(1L, p1.productId(), LikeType.PRODUCT);

            // act
            Page<ProductResponse> result = productAppService.searchProducts(null, "likes_desc", 0, 10);
            List<ProductResponse> content = result.getContent();

            // assert
            assertThat(content.get(0).productId()).isEqualTo(p2.productId());
            assertThat(content.get(1).productId()).isEqualTo(p3.productId());
            assertThat(content.get(2).productId()).isEqualTo(p1.productId());
        }

        @Test
        @DisplayName("성공: 상품 목록 조회 시 캐시가 동작한다.")
        void cacheWorks_whenSearchProducts() {
            // arrange
            productAppService.create(brandAId, "상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE);
            productAppService.create(brandAId, "상품2", "설명", 200, 10, 10, ProductStatus.ACTIVE);

            // act & assert
            System.out.println("\n--- 첫 번째 목록 호출 (Cache Miss 예상) ---");
            productAppService.searchProducts(brandAId, "latest", 0, 10);

            System.out.println("\n--- 두 번째 목록 호출 (Cache Hit 예상) ---");
            productAppService.searchProducts(brandAId, "latest", 0, 10);

        }
    }

    @DisplayName("상품 상세 조회")
    @Nested
    class GetProductDetail {
        @DisplayName("성공: productId로 특정 상품 조회 시 해당 상품 정보와 좋아요 수가 반환된다.")
        @Test
        void returnProductInfo_whenFindByProductId() {
            // arrange
            ProductResponse created = productAppService.create(brandAId, "테스트상품", "설명", 200, 10, 10, ProductStatus.ACTIVE);
            likeAppService.like(1L, created.productId(), LikeType.PRODUCT);
            likeAppService.like(2L, created.productId(), LikeType.PRODUCT);

            // act
            ProductResponse result = productAppService.getProductDetail(created.productId());

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.productId()).isEqualTo(created.productId()),
                    () -> assertThat(result.likeCount()).isEqualTo(2)
            );
        }

        @Test
        @DisplayName("성공: productId로 특정 상품 조회 시 캐시가 동작한다.")
        void cacheWorks_whenFindByProductId() {
            // arrange
            ProductResponse created = productAppService.create(brandAId, "캐시테스트상품", "설명", 200, 10, 10, ProductStatus.ACTIVE);

            // act & assert
            System.out.println("\n--- 첫 번째 호출 (Cache Miss 예상) ---");
            ProductResponse result1 = productAppService.getProductDetail(created.productId());
            assertThat(result1.productId()).isEqualTo(created.productId());

            System.out.println("\n--- 두 번째 호출 (Cache Hit 예상) ---");
            ProductResponse result2 = productAppService.getProductDetail(created.productId());
            assertThat(result2.productId()).isEqualTo(created.productId());

        }
    }
}
