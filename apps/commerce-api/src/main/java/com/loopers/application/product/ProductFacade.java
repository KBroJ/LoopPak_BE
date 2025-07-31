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

    /**
     * 상품 목록을 '좋아요' 수와 함께 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(Long brandId, String sort, int page, int size) {

        // 1. ProductService를 통해 상품 페이징 결과를 가져옵니다.
        Page<Product> productPage = productService.productList(brandId, sort, page, size);

        // 2. 조회된 상품이 없다면 빈 페이지를 반환합니다.
        if (productPage.isEmpty()) {
            return Page.empty();
        }

        // 3. 상품 ID 목록을 추출합니다.
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        // 4. LikeService에서 좋아요 수 Map을 가져옵니다.
        Map<Long, Long> likeCounts = likeService.getLikeCounts(productIds);

        // 5. Page<Product>를 Page<ProductResponse>로 변환합니다.
        return productPage.map(product ->
                new ProductResponse(product, likeCounts.getOrDefault(product.getId(), 0L))
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getLikedProducts(Long userId, LikeType likeType, int page, int size) {
        // 1. LikeService를 통해 좋아요 한 상품 ID 목록을 조회합니다.
        List<Long> likedProductIds = likeService.likeList(userId, likeType);

        // 2. 페이징 정보를 생성합니다. (좋아요 목록은 보통 최신순으로 보여줍니다)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. ProductService에 ID 목록과 페이징 정보를 전달하여 최종 상품 정보를 조회합니다.
        return productService.findProductsByIds(likedProductIds, pageable);
    }



}
