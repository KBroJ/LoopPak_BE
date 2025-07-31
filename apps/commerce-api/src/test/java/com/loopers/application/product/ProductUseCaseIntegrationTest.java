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
        void returnAllActiveProducts_whenSearchWithDefaultConditions() throws InterruptedException {

            // arrange
            Product activeProduct1 = Product.of(brandAId, "활성상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE);
            Product activeProduct2 = Product.of(brandBId, "활성상품2", "설명", 200, 10, 10, ProductStatus.ACTIVE);
            Product inactiveProduct = Product.of(brandAId, "비활성상품", "설명", 300, 10, 10, ProductStatus.INACTIVE);
            Product outOfStockProduct = Product.of(brandBId, "품절상품", "설명", 400, 0, 10, ProductStatus.OUT_OF_STOCK);
            productService.create(activeProduct1);
            Thread.sleep(10);
            Product latestActiveProduct = productService.create(activeProduct2);
            productService.create(inactiveProduct);
            productService.create(outOfStockProduct);

            // act
            Page<Product> result = productService.productList(null, "latest", 0, 20);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(2); // 활성화된 상품 총 2개
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getName()).isEqualTo(latestActiveProduct.getName()); // 첫번째 상품이 가장 최신 상품인지 확인

            // 모든 상품의 상태가 ACTIVE인지 추가로 확인
            result.forEach(product -> assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE));
        }

        @DisplayName("brandId로 필터링 시 해당 브랜드의 활성화된 상품만 반환된다.")
        @Test
        void returnFilteredProducts_whenSearchWithBrandId() {
            // arrange
            productService.create(Product.of(brandAId, "A브랜드상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE));
            productService.create(Product.of(brandAId, "A브랜드상품2-비활성", "설명", 150, 10, 10, ProductStatus.INACTIVE));
            productService.create(Product.of(brandBId, "B브랜드상품1", "설명", 200, 10, 10, ProductStatus.ACTIVE));

            // act: brandId=brandAId, sort="latest", page=0, size=10
            Page<Product> result = productService.productList(brandAId, "latest", 0, 10);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(1); // A브랜드의 활성화 상품은 1개
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("A브랜드상품1");
        }

        @DisplayName("가격 오름차순으로 정렬 시 상품이 가격순으로 반환된다.")
        @Test
        void returnSortedProducts_whenSearchWithPriceAsc() {
            // arrange
            productService.create(Product.of(brandAId, "중간가격상품", "설명", 200, 10, 10, ProductStatus.ACTIVE));
            productService.create(Product.of(brandAId, "최고가상품", "설명", 300, 10, 10, ProductStatus.ACTIVE));
            productService.create(Product.of(brandAId, "최저가상품", "설명", 100, 10, 10, ProductStatus.ACTIVE));

            // act: brandId=null, sort="price_asc", page=0, size=10
            Page<Product> result = productService.productList(null, "price_asc", 0, 10);

            // assert
            assertThat(result.getContent()).hasSize(3);
            // 가격 오름차순으로 정렬되었는지 확인
            assertThat(result.getContent()).isSortedAccordingTo(Comparator.comparing(Product::getPrice));
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

        @DisplayName("내가 좋아요한 상품정보 목록을 조회한다.")
        @Test
        void returnLikedProductsInfo_whenFindMyLikedProducts() {
            // arrange
            Product product1 = productService.create(Product.of(brandAId, "활성상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE));
            Product product2 = productService.create(Product.of(brandBId, "활성상품2", "설명", 200, 10, 10, ProductStatus.ACTIVE));
            productService.create(Product.of(brandAId, "비활성상품", "설명", 300, 10, 10, ProductStatus.INACTIVE));

            likeService.like(1l, product1.getId(), LikeType.PRODUCT);
            likeService.like(1l, product2.getId(), LikeType.PRODUCT);

            // act
            Page<Product> result = productFacade.getLikedProducts(1l, LikeType.PRODUCT, 0, 10);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(2); // 좋아요 한 상품은 총 2개
            // ID를 추출하여 좋아요 한 상품들의 ID와 일치하는지 확인 (좋아요 안 한 상품은 없는지 확인)
            assertThat(result.getContent()).extracting("id")
                    .containsExactlyInAnyOrder(product1.getId(), product2.getId());
        }

        @DisplayName("좋아요 한 상품이 없을 경우, 빈 페이지가 반환된다.")
        @Test
        void returnEmptyPage_whenUserHasNoLikes() {
            // arrange
            Long userWithNoLikes = 2L;

            // act
            Page<Product> result = productFacade.getLikedProducts(userWithNoLikes, LikeType.PRODUCT, 0, 10);

            // assert
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
        }


    }

}
