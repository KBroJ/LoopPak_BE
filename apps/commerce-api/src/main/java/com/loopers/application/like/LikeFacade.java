package com.loopers.application.like;

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

@Component
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeService likeService;
    private final ProductService productService;

    /**
     * 상품에 '좋아요'를 등록합니다.
     */
    @Transactional
    public void likeProduct(Long userId, Long productId) {
        likeService.like(userId, productId, LikeType.PRODUCT);
    }

    /**
     * 상품 '좋아요'를 취소합니다.
     */
    @Transactional
    public void unlikeProduct(Long userId, Long productId) {
        likeService.unLike(userId, productId, LikeType.PRODUCT);
    }

    /**
     * 사용자가 좋아요 한 상품 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<Product> getLikedProducts(Long userId, int page, int size) {
        List<Long> likedProductIds = likeService.likeList(userId, LikeType.PRODUCT);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return productService.findProductsByIds(likedProductIds, pageable);
    }


}
