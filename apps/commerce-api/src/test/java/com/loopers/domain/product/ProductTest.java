package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("product 객체 생성 단위 테스트")
@Nested
class ProductTest {

    Long BRAND_ID = 1l;
    String NAME = "Test Product";
    String DESCRIPTION = "This is a test product.";
    long PRICE = 19;
    int STOCK = 100;
    int MAX_ORDER_QUANTITY = 3;
    ProductStatus STATUS = ProductStatus.ACTIVE;

    @DisplayName("상품 생성")
    @Test
    void productCreat() {
        // Arrange

        // Act
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS
        );

        // Assert
        Assertions.assertThat(product).isNotNull();
        Assertions.assertThat(product.getName()).isEqualTo(NAME);
        Assertions.assertThat(product.getPrice()).isEqualTo(PRICE);

    }

    @DisplayName("brandId가 null일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenBrandIdIsNull() {

        // Arrange
        Long brandId = null;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.of(brandId, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 이름이 null이거나 빈 문자열일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenNameIsNull() {

        // Arrange

        String name = "";

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.of(BRAND_ID, name, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 가격이 0 미만일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenPriceIsZeroOrNegative() {

        // Arrange
        long price = -1;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.of(BRAND_ID, NAME, DESCRIPTION, price, STOCK, MAX_ORDER_QUANTITY, STATUS);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 재고가 0 미만일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenStockIsZeroOrNegative() {

        // Arrange
        int stock = -1;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.of(BRAND_ID, NAME, DESCRIPTION, PRICE, stock, MAX_ORDER_QUANTITY, STATUS);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("최대 주문 수량이 0 이하일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenMaxOrderQuantityIsZeroOrNegative() {

        // Arrange
        int maxOrderQuantity = -1;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.of(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, maxOrderQuantity, STATUS);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 상태가 null일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenStatusIsNull() {

        // Arrange
        ProductStatus status = null;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.of(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, status);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 가격 인상")
    @Test
    void increasePrice() {
        // Arrange
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS
        );

        // Act
        product.increasePrice(5);

        // Assert
        Assertions.assertThat(product.getPrice()).isEqualTo(24);
    }

    @DisplayName("상품 가격 인하")
    @Test
    void decreasePrice() {
        // Arrange
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS
        );

        // Act
        product.decreasePrice(5);

        // Assert
        Assertions.assertThat(product.getPrice()).isEqualTo(14);
    }

    @DisplayName("상품 재고 증가")
    @Test
    void increaseStock() {
        // Arrange
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS
        );

        // Act
        product.increaseStock(50);

        // Assert
        Assertions.assertThat(product.getStock()).isEqualTo(150);
    }

    @DisplayName("상품 재고 감소")
    @Test
    void decreaseStock() {
        // Arrange
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS
        );

        // Act
        product.decreaseStock(30);

        // Assert
        Assertions.assertThat(product.getStock()).isEqualTo(70);
    }

    @DisplayName("상품 활성화")
    @Test
    void activateProduct() {
        // Arrange
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, ProductStatus.INACTIVE
        );

        // Act
        product.activate();

        // Assert
        Assertions.assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @DisplayName("상품 비활성화")
    @Test
    void deactivateProduct() {
        // Arrange
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS
        );

        // Act
        product.deactivate();

        // Assert
        Assertions.assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @DisplayName("상품 재고 없음")
    @Test
    void throwsBadRequestException_whenStockIsOut() {
        // Arrange
        Product product = Product.of(
                BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS
        );

        // Act
        product.outOfStock();

        // Assert
        Assertions.assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @DisplayName("상품 생성 시 likeCount는 0으로 초기화된다.")
    @Test
    void likeCount_isInitializedToZero_whenProductIsCreated() {
        // Arrange & Act
        Product product = Product.of(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS);

        // Assert
        Assertions.assertThat(product.getLikeCount()).isZero();
    }

    @DisplayName("좋아요 수 증가/감소")
    @Nested
    class LikeCountManagement {
        @Test
        @DisplayName("increaseLikeCount 호출 시 likeCount가 1 증가한다.")
        void increaseLikeCount() {
            // Arrange
            Product product = Product.of(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS);

            // Act
            product.increaseLikeCount();

            // Assert
            Assertions.assertThat(product.getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("decreaseLikeCount 호출 시 likeCount가 1 감소한다.")
        void decreaseLikeCount() {
            // Arrange
            Product product = Product.of(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS);
            product.increaseLikeCount(); // 먼저 1로 만듦

            // Act
            product.decreaseLikeCount();

            // Assert
            Assertions.assertThat(product.getLikeCount()).isZero();
        }

        @Test
        @DisplayName("likeCount가 0일 때 decreaseLikeCount를 호출해도 0 미만으로 내려가지 않는다.")
        void doesNotDecreaseBelowZero() {
            // Arrange
            Product product = Product.of(BRAND_ID, NAME, DESCRIPTION, PRICE, STOCK, MAX_ORDER_QUANTITY, STATUS);

            // Act
            product.decreaseLikeCount();

            // Assert
            Assertions.assertThat(product.getLikeCount()).isZero();
        }
    }

}
