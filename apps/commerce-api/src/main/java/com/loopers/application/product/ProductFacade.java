package com.loopers.application.product;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;
    private final LikeService likeService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(Long brandId, String sort, int page, int size) {

        Page<Product> productPage = productService.productList(brandId, sort, page, size);

        if (productPage.isEmpty()) {
            return Page.empty();
        }

        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        Map<Long, Long> likeCounts = likeService.getLikeCounts(productIds);

        List<ProductResponse> productResponses = productPage.getContent().stream()
                .map(product -> new ProductResponse(product, likeCounts.getOrDefault(product.getId(), 0L)))
                .collect(Collectors.toList());

        if ("likes_desc".equals(sort)) {
            productResponses.sort(Comparator.comparing(ProductResponse::getLikeCount).reversed());
        }

        return new PageImpl<>(productResponses, productPage.getPageable(), productPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId) {

        Product product = productService.productInfo(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보를 찾을 수 없습니다."));

        Map<Long, Long> likeCounts = likeService.getLikeCounts(Collections.singletonList(productId));
        long likeCount = likeCounts.getOrDefault(productId, 0L);

        return new ProductResponse(product, likeCount);
    }

}
