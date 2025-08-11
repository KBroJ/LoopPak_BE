package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Brand 객체 생성 단위 테스트")
@Nested
class BrandTest {

    @DisplayName("Brand 객체 생성")
    @Test
    void ofBrand() {
        // Arrange
        String name = "Test Brand";
        String description = "This is a test brand.";
        Boolean isActive = true;

        // Act
        Brand brand = Brand.of(name, description, isActive);

        // Assert
        assertNotNull(brand);
        assertEquals(name, brand.getName());
        assertEquals(description, brand.getDescription());
        assertEquals(isActive, brand.getIsActive());
    }

    @DisplayName("Name값이 null이거나 빈 문자열일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenNameIsNull() {
        // Arrange
        String name = "";
        String description = "This is a test brand.";
        Boolean isActive = true;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Brand brand = Brand.of(name, description, isActive);
        });

        // Assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

    }

    @DisplayName("isActive값이 null일 때 예외 발생")
    @Test
    void throwsBadRequestException_whenIsActiveIsNull() {
        // Arrange
        String name = "Test Brand";
        String description = "This is a test brand.";
        Boolean isActive = null;

        // Act
        CoreException result = assertThrows(CoreException.class, () -> {
            Brand brand = Brand.of(name, description, isActive);
        });

        // Assert
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

}
