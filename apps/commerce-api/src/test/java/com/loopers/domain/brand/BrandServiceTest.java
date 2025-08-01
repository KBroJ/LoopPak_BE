package com.loopers.domain.brand;

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
class BrandServiceTest {

    @InjectMocks
    private BrandService brandService;

    @Mock
    private BrandRepository brandRepository;

    @Test
    @DisplayName("브랜드 생성 시, save 메서드를 호출하고 생성된 브랜드를 반환한다.")
    void create_callsSaveAndReturnsBrand() {
        // given
        Brand brand = Brand.of("Test Brand", "Description", true);
        given(brandRepository.save(any(Brand.class))).willReturn(brand);

        // when
        Brand result = brandService.create(brand);

        // then
        assertThat(result).isEqualTo(brand);
        verify(brandRepository, times(1)).save(brand);
    }

    @Test
    @DisplayName("브랜드 조회 시, ID가 존재하면 브랜드를 반환한다.")
    void brandInfo_returnsBrand_whenIdExists() {
        // given
        Long brandId = 1L;
        Brand brand = Brand.of("Test Brand", "Description", true);
        given(brandRepository.findById(brandId)).willReturn(Optional.of(brand));

        // when
        Brand result = brandService.brandInfo(brandId);

        // then
        assertThat(result).isEqualTo(brand);
    }

    @Test
    @DisplayName("브랜드 조회 시, ID가 없으면 예외를 던진다.")
    void brandInfo_throwsException_whenIdDoesNotExist() {
        // given
        Long brandId = 99L;
        given(brandRepository.findById(brandId)).willReturn(Optional.empty());

        // when & then
        assertThrows(CoreException.class, () -> brandService.brandInfo(brandId));
    }

}
