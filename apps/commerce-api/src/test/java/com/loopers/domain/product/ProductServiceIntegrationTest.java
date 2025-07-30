package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductServiceIntegrationTest {

    @Autowired
    private BrandService brandService;
    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    Brand brandInfo = Brand.of("루퍼스", "백엔드 실력 성장 - 이직하고 싶다", true);
    private Brand BRAND;
    @BeforeEach
    void createBrand() {
        BRAND = brandService.create(brandInfo);
    }

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }

    private static final String NAME = "상품명";
    private static final String DESCRIPTION = "상품설명";
    private static final long PRICE = 100;
    private static final int STOCK = 10;
    private static final int MAX_ORDER_QUANTITIY = 10;
    private static final ProductStatus STATUS = ProductStatus.ACTIVE;

    @DisplayName("상품 생성 시 상품 정보가 반환된다.")
    @Test
    void returnProductInfo_whenCreateProduct() {

        // arrange
        Product product = Product.of(
            BRAND, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITIY, STATUS
        );

        // act
        Product result = productService.create(product);

        // assert
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getBrand().getId()).isEqualTo(BRAND.getId()),
            () -> assertThat(result.getId()).isNotNull(),
            () -> assertThat(result.getName()).isEqualTo(NAME),
            () -> assertThat(result.getDescription()).isEqualTo(DESCRIPTION),
            () -> assertThat(result.getPrice()).isEqualTo(PRICE),
            () -> assertThat(result.getStock()).isEqualTo(STOCK),
            () -> assertThat(result.getMaxOrderQuantity()).isEqualTo(MAX_ORDER_QUANTITIY),
            () -> assertThat(result.getStatus()).isEqualTo(STATUS)
        );


    }

    @DisplayName("정보 조회")
    @Nested
    class findInfo {

        @DisplayName("상품 목록 조회 시 STATUS가 ACTIVE 상태인 상품 목록 리스트가 반환된다.")
        @Test
        void returnProductList_whenGetProductList() {

            // arrange
            Product activeProduct1 = Product.of(BRAND, "활성상품1", "설명", 100, 10, 10, ProductStatus.ACTIVE);
            Product activeProduct2 = Product.of(BRAND, "활성상품2", "설명", 200, 10, 10, ProductStatus.ACTIVE);
            Product inactiveProduct = Product.of(BRAND, "비활성상품", "설명", 300, 10, 10, ProductStatus.INACTIVE);
            Product outOfStockProduct = Product.of(BRAND, "품절상품", "설명", 400, 0, 10, ProductStatus.OUT_OF_STOCK);
            productService.create(activeProduct1);
            productService.create(activeProduct2);
            productService.create(inactiveProduct);
            productService.create(outOfStockProduct);

            // act
            List<Product> result = productService.productList();

            // assert
            assertThat(result).hasSize(2) // ACTIVE 상태인 상품은 2개여야 함
                    .extracting("name") // 상품 이름만 추출해서
                    .containsExactlyInAnyOrder("활성상품1", "활성상품2"); // 이름이 맞는지 순서 상관없이 확인

            // 모든 상품의 상태가 ACTIVE인지 추가로 확인
            result.forEach(product -> assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE));
        }

    }

}
