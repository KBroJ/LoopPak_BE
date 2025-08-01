package com.loopers.application.product;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

        return productPage.map(product ->
                new ProductResponse(product, likeCounts.getOrDefault(product.getId(), 0L))
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getLikedProducts(Long userId, LikeType likeType, int page, int size) {
        List<Long> likedProductIds = likeService.likeList(userId, likeType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return productService.findProductsByIds(likedProductIds, pageable);
    }



}
