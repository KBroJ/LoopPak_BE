package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;

@Tag(name = "상품 API", description = "상품 목록 및 상세 정보 조회 API")
public interface ProductV1ApiSpec {

    @Operation(summary = "상품 목록 조회", description = "다양한 조건으로 상품 목록을 조회합니다.")
    ApiResponse<Page<ProductV1Dto.Summary>> getProducts(
            @Parameter(description = "브랜드 ID로 필터링") Long brandId,
            @Parameter(description = "정렬 기준 (latest, price_asc, likes_desc 중 하나)") String sort,
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지당 개수") int size
    );

    @Operation(summary = "상품 상세 정보 조회")
    ApiResponse<ProductV1Dto.Detail> getProduct(
            @Parameter(description = "조회할 상품 ID") Long productId
    );

}
