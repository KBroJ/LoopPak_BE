package com.loopers.domain.metrics;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * ProductMetrics 도메인 Repository 인터페이스
 * 도메인 관점에서의 상품 집계 저장소 추상화
 */
public interface ProductMetricsRepository {

    /**
     * 상품 집계 저장 및 업데이트
     */
    ProductMetrics save(ProductMetrics productMetrics);

    /**
     * 상품 ID로 집계 정보 조회
     */
    Optional<ProductMetrics> findByProductId(Long productId);

    /**
     * 좋아요 수 증감 (UPSERT)
     * @param productId 상품 ID
     * @param delta +1(증가) 또는 -1(감소)
     */
    void updateLikeCount(Long productId, int delta);

    /**
     * 조회수 증가 (UPSERT, 향후 확장용)
     */
    void updateViewCount(Long productId);

    /**
     * 판매량 증가 (UPSERT)
     * @param productId 상품 ID
     * @param quantity 증가할 판매량 (1개 이상)
     */
    void updateSalesCount(Long productId, int quantity);

    /**
     * 좋아요 수 기준 인기 상품 TOP N 조회
     */
    List<ProductMetrics> findTopLikedProducts(Pageable pageable);

    /**
     * 최소 좋아요 수 이상인 상품 목록 조회
     */
    List<ProductMetrics> findProductsWithMinLikeCount(int minLikeCount);

    /**
     * 전체 상품 수 조회
     */
    Long countTotalProducts();

}
