package com.loopers.domain.brand;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional
    public Brand create(Brand brand) {
        return brandRepository.save(brand);
    }

    @Transactional(readOnly = true)
    public Optional<Brand> brandInfo(Long brandId) {
        return brandRepository.findById(brandId);
    }

}
