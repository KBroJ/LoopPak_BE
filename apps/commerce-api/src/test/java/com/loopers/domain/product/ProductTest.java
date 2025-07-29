package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
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

    String brandName = "Test Brand";
    String brandDescription = "This is a test brand.";
    Boolean isActive = true;
    Brand brand = Brand.create(
            brandName, brandDescription, isActive
    );

    @DisplayName("상품 생성")
    @Test
    void productCreat() {
        // Arrange
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;

        // Act
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
        );

        // Assert
        Assertions.assertThat(product).isNotNull();
        Assertions.assertThat(product.getName()).isEqualTo(name);
        Assertions.assertThat(product.getPrice()).isEqualTo(price);

    }

    @DisplayName("브랜드가 null일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenBrandIsNull() {

        // Arrange
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;
        Brand brand = null; // 브랜드가 null

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.create(brand, name, description, price, stock, maxOrderQuantity, status);
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
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.create(brand, name, description, price, stock, maxOrderQuantity, status);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 가격이 0 미만일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenPriceIsZeroOrNegative() {

        // Arrange
        String name = "Test Product";
        String description = "This is a test product.";
        long price = -1; // 0 미만의 가격
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.create(brand, name, description, price, stock, maxOrderQuantity, status);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 재고가 0 미만일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenStockIsZeroOrNegative() {

        // Arrange
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = -1; // 0 미만의 재고
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.create(brand, name, description, price, stock, maxOrderQuantity, status);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("최대 주문 수량이 0 이하일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenMaxOrderQuantityIsZeroOrNegative() {

        // Arrange
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 0; // 0 이하의 최대 주문 수량
        ProductStatus status = ProductStatus.ACTIVE;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.create(brand, name, description, price, stock, maxOrderQuantity, status);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 상태가 null일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenStatusIsNull() {

        // Arrange
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = null; // 상태가 null

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Product.create(brand, name, description, price, stock, maxOrderQuantity, status);
        });

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("상품 가격 인상")
    @Test
    void increasePrice() {
        // Arrange
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
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
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
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
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
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
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
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
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.INACTIVE; // 비활성 상태
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
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
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 100;
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE; // 활성 상태
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
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
        String name = "Test Product";
        String description = "This is a test product.";
        long price = 19;
        int stock = 0; // 재고 없음
        int maxOrderQuantity = 3;
        ProductStatus status = ProductStatus.ACTIVE;
        Product product = Product.create(
                brand, name, description, price, stock, maxOrderQuantity, status
        );

        // Act
        product.outOfStock();

        // Assert
        Assertions.assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

}
