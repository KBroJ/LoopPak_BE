package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProductMetricsRepository 구현체
 * JPA를 통한 상품 집계 데이터 접근 구현
 */
@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    @Override
    public ProductMetrics save(ProductMetrics productMetrics) {
        return productMetricsJpaRepository.save(productMetrics);
    }

    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return productMetricsJpaRepository.findByProductId(productId);
    }

    @Override
    public void updateLikeCount(Long productId, int delta) {
        productMetricsJpaRepository.upsertLikeCount(productId, delta);
    }

    @Override
    public void updateViewCount(Long productId) {
        productMetricsJpaRepository.upsertViewCount(productId);
    }

    @Override
    public void updateSalesCount(Long productId, int quantity) {
        productMetricsJpaRepository.upsertSalesCount(productId, quantity);
    }

    @Override
    public List<ProductMetrics> findTopLikedProducts(Pageable pageable) {
        return productMetricsJpaRepository.findTopLikedProducts(pageable);
    }

    @Override
    public List<ProductMetrics> findProductsWithMinLikeCount(int minLikeCount) {
        return productMetricsJpaRepository.findProductsWithMinLikeCount(minLikeCount);
    }

    @Override
    public Long countTotalProducts() {
        return productMetricsJpaRepository.countTotalProducts();
    }

}
