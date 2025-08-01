package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BrandV1Controller implements BrandV1ApiSpec {

    private final BrandFacade brandFacade;

    @GetMapping("/api/v1/brands/{brandId}")
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(
            @PathVariable Long brandId
    ) {

        BrandInfo brandInfo = brandFacade.getBrand(brandId);
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(brandInfo);
        return ApiResponse.success(response);

    }

}
