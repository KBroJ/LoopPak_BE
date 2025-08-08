package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BrandApplicationServiceTest {

    @InjectMocks
    private BrandApplicationService brandApplicationService;

    @Mock
    private BrandRepository brandRepository;

    @Test
    @DisplayName("브랜드 생성 시, save 메서드를 호출하고 생성된 브랜드 정보를 DTO로 반환한다.")
    void create_callsSaveAndReturnsBrand() {
        // arrange
        String name = "Test Brand";
        String description = "Description";
        boolean isActive = true;

        // Mock 객체가 반환할 Brand Entity를 준비
        Brand savedBrand = Brand.of(name, description, isActive);
        given(brandRepository.save(any(Brand.class))).willReturn(savedBrand);

        // act
        BrandInfo result = brandApplicationService.create(name, description, isActive);

        // assert
        // 반환된 DTO의 내용 검증
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.description()).isEqualTo(description);
        verify(brandRepository, times(1)).save(any(Brand.class));
    }

    @Test
    @DisplayName("브랜드 조회 시, ID가 존재하면 브랜드 정보를 DTO로 반환한다.")
    void getBrand_returnsBrandInfo_whenIdExists() {
        // arrange
        Long brandId = 1L;
        Brand brand = Brand.of("Test Brand", "Description", true);
        given(brandRepository.findById(brandId)).willReturn(Optional.of(brand));

        // act
        BrandInfo result = brandApplicationService.getBrand(brandId);

        // assert
        // 반환된 DTO의 내용 검증
        assertThat(result.name()).isEqualTo(brand.getName());
        assertThat(result.description()).isEqualTo(brand.getDescription());
    }

    @Test
    @DisplayName("브랜드 조회 시, ID가 없으면 예외를 던진다.")
    void getBrand_throwsException_whenIdDoesNotExist() {
        // arrange
        Long brandId = 99L;
        given(brandRepository.findById(brandId)).willReturn(Optional.empty());

        // act & then
        assertThrows(CoreException.class, () -> brandApplicationService.getBrand(brandId));
    }


}
