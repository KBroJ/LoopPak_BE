package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ProductMetrics JPA Repository
 * Spring Data JPA 기반 상품 집계 데이터 접근
 */
public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    /**
     * 상품 ID로 집계 정보 조회
     */
    Optional<ProductMetrics> findByProductId(Long productId);

    /**
     * 좋아요 수 증감 (UPSERT)
     * 상품 집계가 없으면 생성하고, 있으면 좋아요 수 증감
     */
    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, view_count, sales_count, created_at, updated_at)
        VALUES (:productId, :delta, 0, 0, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            like_count = GREATEST(0, like_count + :delta),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertLikeCount(@Param("productId") Long productId, @Param("delta") int delta);

    /**
     * 조회수 증가 (UPSERT)
     */
    @Modifying
    @Query(value = """
          INSERT INTO product_metrics (product_id, like_count, view_count, sales_count, created_at, updated_at)
          VALUES (:productId, 0, 1, 0, NOW(), NOW())
          ON DUPLICATE KEY UPDATE
              view_count = view_count + 1,
              updated_at = NOW()
          """, nativeQuery = true)
    void upsertViewCount(@Param("productId") Long productId);

    /**
     * 판매량 증가 (UPSERT)
     * 실제 판매된 수량을 정확히 반영
     */
    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, view_count, sales_count, created_at, updated_at)
        VALUES (:productId, 0, 0, :quantity, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            sales_count = sales_count + :quantity,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertSalesCount(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 좋아요 수 기준 인기 상품 조회 (Pageable)
     */
    @Query("SELECT pm FROM ProductMetrics pm ORDER BY pm.likeCount DESC")
    List<ProductMetrics> findTopLikedProducts(Pageable pageable);

    /**
     * 최소 좋아요 수 이상인 상품 목록 조회
     */
    @Query("SELECT pm FROM ProductMetrics pm WHERE pm.likeCount >= :minLikeCount ORDER BY pm.likeCount DESC")
    List<ProductMetrics> findProductsWithMinLikeCount(@Param("minLikeCount") int minLikeCount);

    /**
     * 전체 상품 수 조회
     */
    @Query("SELECT COUNT(pm) FROM ProductMetrics pm")
    Long countTotalProducts();

}
