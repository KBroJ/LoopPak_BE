package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.product.Product;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/like/products")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/{productId}")
    @Override
    public ApiResponse<Object> like(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long productId) {

        likeFacade.likeProduct(userId, productId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{productId}")
    @Override
    public ApiResponse<Object> unlike(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long productId) {

        likeFacade.unlikeProduct(userId, productId);
        return ApiResponse.success();
    }

    @GetMapping
    @Override
    public ApiResponse<Page<LikeV1Dto.Product>> getLikedProducts(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Product> likedProducts = likeFacade.getLikedProducts(userId, page, size);
        Page<LikeV1Dto.Product> response = likedProducts.map(LikeV1Dto.Product::from);

        return ApiResponse.success(response);
    }
}
