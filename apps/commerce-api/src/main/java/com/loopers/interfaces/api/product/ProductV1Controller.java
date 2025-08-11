package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductApplicationService;
import com.loopers.application.product.ProductResponse;
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

    private final ProductApplicationService productApplicationService;

    @GetMapping("/api/v1/products")
    @Override
    public ApiResponse<Page<ProductV1Dto.Summary>> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ProductResponse> productPage = productApplicationService.searchProducts(brandId, sort, page, size);
        Page<ProductV1Dto.Summary> response = productPage.map(ProductV1Dto.Summary::from);

        return ApiResponse.success(response);
    }

    @GetMapping("/api/v1/products/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.Detail> getProduct(@PathVariable Long productId) {
        ProductResponse productResponse = productApplicationService.getProductDetail(productId);

        ProductV1Dto.Detail response = ProductV1Dto.Detail.from(productResponse);
        return ApiResponse.success(response);
    }
}
