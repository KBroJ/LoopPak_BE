package com.loopers.interfaces.api.product;

import com.loopers.application.product.PageResponse;
import com.loopers.application.product.ProductApplicationService;
import com.loopers.application.product.ProductResponse;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductApplicationService productApplicationService;

    @GetMapping("/api/v1/products")
    @Override
    public ApiResponse<PageResponse<ProductV1Dto.Summary>> getProducts(
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<ProductResponse> serviceResponse = productApplicationService.searchProducts(brandId, sort, page, size);

        // content 목록만 ProductResponse에서 ProductV1Dto.Summary로 변환
        List<ProductV1Dto.Summary> summaryList = serviceResponse.content().stream()
                .map(ProductV1Dto.Summary::from)
                .toList();

        // 3. 변환된 content와 기존 페이지 정보로 새로운 PageResponse를 만들어 최종 응답
        PageResponse<ProductV1Dto.Summary> finalResponse = new PageResponse<>(
                summaryList,
                serviceResponse.page(),
                serviceResponse.size(),
                serviceResponse.totalPages(),
                serviceResponse.totalElements()
        );

        return ApiResponse.success(finalResponse);
    }

    @GetMapping("/api/v1/products/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.Detail> getProduct(@PathVariable Long productId) {
        ProductResponse productResponse = productApplicationService.getProductDetail(productId);

        ProductV1Dto.Detail response = ProductV1Dto.Detail.from(productResponse);
        return ApiResponse.success(response);
    }
}
