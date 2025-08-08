package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandApplicationService {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandInfo create(String name, String description, Boolean isActive) {
        Brand brand = Brand.of(name, description, isActive);
        Brand savedBrand = brandRepository.save(brand);
        return BrandInfo.from(savedBrand);
    }

    @Transactional(readOnly = true)
    public BrandInfo getBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드 정보를 찾을 수 없습니다."));
        return BrandInfo.from(brand);
    }

}
