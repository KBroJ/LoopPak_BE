package com.loopers.collector.application.metrics;

import com.loopers.collector.domain.metrics.ProductMetrics;
import com.loopers.collector.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 집계 데이터 처리 서비스
 * 이벤트 기반으로 상품의 좋아요, 조회수, 주문수 등을 집계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final ProductMetricsRepository productMetricsRepository;

    /**
     * 상품 좋아요 수 증가
     * LikeAddedEvent 처리 시 호출
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void increaseLikeCount(Long productId) {
        try {
            productMetricsRepository.updateLikeCount(productId, +1);
            log.info("상품 좋아요 수 증가 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("상품 좋아요 수 증가 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 상품 좋아요 수 감소
     * LikeRemovedEvent 처리 시 호출
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void decreaseLikeCount(Long productId) {
        try {
            productMetricsRepository.updateLikeCount(productId, -1);
            log.info("상품 좋아요 수 감소 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("상품 좋아요 수 감소 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 상품 조회수 증가
     * ProductViewedEvent 처리 시 호출
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void increaseViewCount(Long productId) {
        try {
            productMetricsRepository.updateViewCount(productId);
            log.info("상품 조회수 증가 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("상품 조회수 증가 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 상품 판매량 증가 (기본 1개)
     * StockDecreasedEvent 처리 시 호출
     */
    @Transactional
    public void increaseSalesCount(Long productId) {
        increaseSalesCount(productId, 1);
    }

    /**
     * 상품 판매량 증가 (수량 지정)
     * StockDecreasedEvent에서 실제 판매 수량을 정확히 반영
     */
    @Transactional
    public void increaseSalesCount(Long productId, int quantity) {
        // 비즈니스 로직 검증 (Application Layer 책임)
        if (quantity <= 0) {
            throw new IllegalArgumentException("판매량은 1 이상이어야 합니다: " + quantity);
        }

        try {
            productMetricsRepository.updateSalesCount(productId, quantity);
            log.info("상품 판매량 증가 완료 - productId: {}, quantity: {}", productId, quantity);

        } catch (Exception e) {
            log.error("상품 판매량 증가 실패 - productId: {}, quantity: {}, error: {}",
                    productId, quantity, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 인기 상품 TOP N 조회
     * 분석 및 추천 시스템에서 사용
     *
     * @param limit 조회할 상품 수
     * @return 좋아요 수 기준 상위 상품 목록
     */
    public List<ProductMetrics> getTopLikedProducts(int limit) {
        try {
            return productMetricsRepository.findTopLikedProducts(PageRequest.of(0, limit));

        } catch (Exception e) {
            log.error("인기 상품 조회 실패 - limit: {}, error: {}", limit, e.getMessage(), e);
            throw e;
        }
    }

}
