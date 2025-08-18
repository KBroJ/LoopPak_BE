package com.loopers.interfaces.api.product;

import com.loopers.application.product.*;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductQueryService productQueryService;

    @GetMapping("/api/v1/products")
    @Override
    public ApiResponse<Page<ProductV1Dto.Summary>> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ProductResponse> serviceResponse = productQueryService.searchProducts(brandId, sort, page, size);

        // content 목록만 ProductResponse에서 ProductV1Dto.Summary로 변환
        Page<ProductV1Dto.Summary> finalResponse = serviceResponse.map(ProductV1Dto.Summary::from);

        return ApiResponse.success(finalResponse);
    }

    @GetMapping("/api/v1/products/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.Detail> getProduct(@PathVariable Long productId) {
        ProductResponse productResponse = productQueryService.getProductDetail(productId);

        ProductV1Dto.Detail response = ProductV1Dto.Detail.from(productResponse);
        return ApiResponse.success(response);
    }
}
