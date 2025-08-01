package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "좋아요 API", description = "상품 좋아요 등록/취소/조회 API")
public interface LikeV1ApiSpec {

    @Operation(summary = "상품 좋아요 등록", description = "특정 상품에 좋아요를 등록합니다. (멱등성 보장)")
    ApiResponse<Object> like(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(description = "좋아요할 상품 ID") Long productId
    );

    @Operation(summary = "상품 좋아요 취소", description = "특정 상품의 좋아요를 취소합니다. (멱등성 보장)")
    ApiResponse<Object> unlike(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(description = "좋아요 취소할 상품 ID") Long productId
    );

    @Operation(summary = "내가 좋아요 한 상품 목록 조회")
    ApiResponse<Page<LikeV1Dto.Product>> getLikedProducts(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지당 개수") int size
    );

}
