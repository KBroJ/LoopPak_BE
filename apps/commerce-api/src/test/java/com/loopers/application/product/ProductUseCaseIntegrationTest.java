package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductUseCaseIntegrationTest {

    @Autowired
    private ProductFacade productFacade;
    @Autowired
    private BrandService brandService;
    @Autowired
    private ProductService productService;
    @Autowired
    private LikeService likeService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long brandAId;
    private Long brandBId;
    @BeforeEach
    void setUp() {
        // 각 테스트 전에 독립적인 브랜드 데이터를 생성합니다.
        Brand brandA = brandService.create(Brand.of("브랜드A", "설명", true));
        Brand brandB = brandService.create(Brand.of("브랜드B", "설명", true));
        brandAId = brandA.getId();
        brandBId = brandB.getId();
    }

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }

    @DisplayName("상품 생성")
    @Nested
    class create {
        @DisplayName("상품 생성 시 상품 정보가 반환된다.")
        @Test
        void returnProductInfo_whenCreateProduct() {

            // arrange
            Product product = Product.of(
                    brandAId, "상품명", "설명", 100, 10, 10, ProductStatus.ACTIVE
            );

            // act
            Product result = productService.create(product);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getBrandId()).isEqualTo(brandAId),
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("상품명"),
                () -> assertThat(result.getDescription()).isEqualTo("설명"),
                () -> assertThat(result.getPrice()).isEqualTo(100),
                () -> assertThat(result.getStock()).isEqualTo(10),
                () -> assertThat(result.getMaxOrderQuantity()).isEqualTo(10),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE)
            );

        }
    }

    @DisplayName("상품 목록 조회")
    @Nested
    class productList {

        @DisplayName("기본 조건(brandId 없음, 최신순)으로 조회 시 활성화된 모든 상품이 최신순으로 반환된다.")
        @Test
        void returnAllActiveProductsWithLikeCount_whenSearchWithDefaultConditions() throws InterruptedException {

            // arrange
            Product product1 = productService.create(Product.of(brandAId, "활성상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE));
            Thread.sleep(10);
            Product product2 = productService.create(Product.of(brandBId, "활성상품2", "설명", 200, 10, 10, ProductStatus.ACTIVE));
            productService.create(Product.of(brandAId, "비활성상품", "설명", 300, 10, 10, ProductStatus.INACTIVE));
            productService.create(Product.of(brandBId, "품절상품", "설명", 400, 0, 10, ProductStatus.OUT_OF_STOCK));

            likeService.like(1L, product1.getId(), LikeType.PRODUCT);
            likeService.like(2L, product1.getId(), LikeType.PRODUCT);
            likeService.like(1L, product2.getId(), LikeType.PRODUCT);

            // act
            Page<ProductResponse> result = productFacade.searchProducts(null, "latest", 0, 10);
            List<ProductResponse> content = result.getContent();

            // assert
            assertAll(
                () -> assertThat(result.getTotalElements()).isEqualTo(2),
                () -> assertThat(content).hasSize(2),
                () -> assertThat(content.get(0).getProduct().getId()).isEqualTo(product2.getId()),
                () -> assertThat(content.get(0).getProduct().getName()).isEqualTo(product2.getName()),
                () -> assertThat(content.get(0).getProduct().getDescription()).isEqualTo(product2.getDescription()),
                () -> assertThat(content.get(0).getProduct().getPrice()).isEqualTo(product2.getPrice()),
                () -> assertThat(content.get(0).getProduct().getStock()).isEqualTo(product2.getStock()),
                () -> assertThat(content.get(0).getProduct().getMaxOrderQuantity()).isEqualTo(product2.getMaxOrderQuantity()),
                () -> assertThat(content.get(0).getLikeCount()).isEqualTo(1),
                () -> assertThat(content.get(1).getLikeCount()).isEqualTo(2)
            );

        }



        @DisplayName("brandId로 필터링 시 해당 브랜드의 활성화된 상품만 반환된다.")
        @Test
        void returnFilteredProducts_whenSearchWithBrandId() {
            // arrange
            Product product = productService.create(Product.of(brandAId, "A브랜드상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE));
            productService.create(Product.of(brandAId, "A브랜드상품2-비활성", "설명", 150, 10, 10, ProductStatus.INACTIVE));
            productService.create(Product.of(brandBId, "B브랜드상품1", "설명", 200, 10, 10, ProductStatus.ACTIVE));
            likeService.like(1l, product.getId(), LikeType.PRODUCT);
            likeService.like(2l, product.getId(), LikeType.PRODUCT);

            // act: brandId=brandAId, sort="latest", page=0, size=10
            Page<ProductResponse> result = productFacade.searchProducts(brandAId, "latest", 0, 10);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(1); // A브랜드의 활성화 상품은 1개
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getProduct().getName()).isEqualTo("A브랜드상품1");
            assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(2);

        }

        @DisplayName("가격 오름차순으로 정렬 시 상품이 가격순으로 반환된다.")
        @Test
        void returnSortedProducts_whenSearchWithPriceAsc() {
            // arrange
            Product product_200 = productService.create(Product.of(brandAId, "중간가격상품", "설명", 200, 10, 10, ProductStatus.ACTIVE));
            Product product_300 = productService.create(Product.of(brandAId, "최고가상품", "설명", 300, 10, 10, ProductStatus.ACTIVE));
            Product product_100 = productService.create(Product.of(brandAId, "최저가상품", "설명", 100, 10, 10, ProductStatus.ACTIVE));
            likeService.like(1L, product_100.getId(), LikeType.PRODUCT);
            likeService.like(2L, product_100.getId(), LikeType.PRODUCT);

            // act: brandId=null, sort="price_asc", page=0, size=10
            Page<ProductResponse> resultPage = productFacade.searchProducts(null, "price_asc", 0, 10);
            List<ProductResponse> content = resultPage.getContent();

            // assert
            assertThat(content).hasSize(3);
            assertThat(content)
                    .isSortedAccordingTo(Comparator.comparing(response -> response.getProduct().getPrice()));
            assertThat(content.get(0).getProduct().getName()).isEqualTo(product_100.getName());
            assertThat(content.get(0).getLikeCount()).isEqualTo(2);
            assertThat(content.get(1).getProduct().getName()).isEqualTo(product_200.getName());
            assertThat(content.get(1).getLikeCount()).isZero();
        }

        @DisplayName("좋아요 많은 순으로 정렬 시 상품이 좋아요 수 순으로 반환된다.")
        @Test
        void returnSortedProducts_whenSearchWithLikesDesc() {
            // arrange
            Product p1 = productService.create(Product.of(brandAId, "좋아요1개", "설명", 100, 10, 10, ProductStatus.ACTIVE));
            Product p2 = productService.create(Product.of(brandAId, "좋아요3개", "설명", 100, 10, 10, ProductStatus.ACTIVE));
            Product p3 = productService.create(Product.of(brandAId, "좋아요2개", "설명", 100, 10, 10, ProductStatus.ACTIVE));

            // p2에 좋아요 3개
            likeService.like(1L, p2.getId(), LikeType.PRODUCT);
            likeService.like(2L, p2.getId(), LikeType.PRODUCT);
            likeService.like(3L, p2.getId(), LikeType.PRODUCT);
            // p3에 좋아요 2개
            likeService.like(1L, p3.getId(), LikeType.PRODUCT);
            likeService.like(2L, p3.getId(), LikeType.PRODUCT);
            // p1에 좋아요 1개
            likeService.like(1L, p1.getId(), LikeType.PRODUCT);

            // act
            Page<ProductResponse> result = productFacade.searchProducts(null, "likes_desc", 0, 10);
            List<ProductResponse> content = result.getContent();

            // assert
            assertThat(content).hasSize(3);
            assertThat(content.get(0).getProduct().getId()).isEqualTo(p2.getId()); // 좋아요 3개짜리
            assertThat(content.get(0).getLikeCount()).isEqualTo(3);
            assertThat(content.get(1).getProduct().getId()).isEqualTo(p3.getId()); // 좋아요 2개짜리
            assertThat(content.get(1).getLikeCount()).isEqualTo(2);
            assertThat(content.get(2).getProduct().getId()).isEqualTo(p1.getId()); // 좋아요 1개짜리
            assertThat(content.get(2).getLikeCount()).isEqualTo(1);
        }

        @DisplayName("페이징 처리 시 요청한 페이지와 사이즈에 맞는 결과가 반환된다.")
        @Test
        void returnPagedProducts_whenSearchWithPaging() {
            // arrange: 총 5개의 활성화 상품 생성
            for (int i = 1; i <= 5; i++) {
                productService.create(
                        Product.of(brandAId, "상품" + i, "설명", 100, 10, 10, ProductStatus.ACTIVE)
                );
            }

            // act: 2번째 페이지(page=1), 사이즈 2개 요청
            Page<Product> result = productService.productList(null, "latest", 1, 2);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(5); // 총 상품 수
            assertThat(result.getTotalPages()).isEqualTo(3);    // 총 페이지 수 (5개 / 2 = 2.5 -> 3)
            assertThat(result.getNumber()).isEqualTo(1);        // 현재 페이지 번호 (0부터 시작)
            assertThat(result.getContent()).hasSize(2);         // 현재 페이지의 상품 수
        }

    }

    @DisplayName("상품 정보 조회")
    @Nested
    class productInfo {

        @DisplayName("productId 파라미터로 특정 상품 조회 시 해당 상품 정보가 반환된다.")
        @Test
        void returnProductInfo_whenFindByProductId() {
            // arrange
            Product product = productService.create(
                    Product.of(brandAId, "중간가격상품", "설명", 200, 10, 10, ProductStatus.ACTIVE)
            );

            // act
            Optional<Product> result = productService.productInfo(product.getId());

            // assert
            assertAll(
                () -> assertThat(result.get()).isNotNull(),
                () -> assertThat(result.get().getId()).isEqualTo(product.getId()),
                () -> assertThat(result.get().getName()).isEqualTo(product.getName()),
                () -> assertThat(result.get().getDescription()).isEqualTo(product.getDescription()),
                () -> assertThat(result.get().getPrice()).isEqualTo(product.getPrice()),
                () -> assertThat(result.get().getStock()).isEqualTo(product.getStock()),
                () -> assertThat(result.get().getMaxOrderQuantity()).isEqualTo(product.getMaxOrderQuantity()),
                () -> assertThat(result.get().getStatus()).isEqualTo(product.getStatus())
            );

        }

    }
}
