package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "브랜드 API", description = "브랜드 정보 조회 API")
public interface BrandV1ApiSpec {

    @Operation(summary = "단일 브랜드 정보 조회")
    ApiResponse<BrandV1Dto.BrandResponse> getBrand(
            @Parameter(description = "조회할 브랜드 ID")
            Long brandId
    );

}
