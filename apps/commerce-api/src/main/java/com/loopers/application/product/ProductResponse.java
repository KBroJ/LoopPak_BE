package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;

public record ProductResponse(
        Long productId,
        Long brandId,
        String name,
        String description,
        long price,
        int stock,
        ProductStatus productStatus,
        long likeCount,
        Integer currentRank,        // 현재 순위 (1위부터, 순위에 없으면 null)
        Double currentScore         // 현재 점수 (순위에 없으면 null)
) {

    /**
     * Product 엔티티로부터 ProductResponse 생성 (랭킹 정보 없음)
     *
     * @param product Product 엔티티
     * @return 랭킹 정보가 null인 ProductResponse
     */
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getBrandId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getLikeCount(),
                null,     // 랭킹 정보는 별도 조회 필요
                null                // 점수 정보는 별도 조회 필요
        );
    }

    /**
     * Product 엔티티와 랭킹 정보로부터 ProductResponse 생성
     *
     * @param product Product 엔티티
     * @param currentRank 현재 순위 (순위에 없으면 null)
     * @param currentScore 현재 점수 (순위에 없으면 null)
     * @return 랭킹 정보가 포함된 ProductResponse
     */
    public static ProductResponse withRanking(Product product, Integer currentRank, Double currentScore) {
        return new ProductResponse(
                product.getId(),
                product.getBrandId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getLikeCount(),
                currentRank,
                currentScore
        );
    }

}
