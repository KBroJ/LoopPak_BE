package com.loopers.collector.application.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Redis 캐시 무효화 처리 서비스
 * 이벤트 발생 시 관련된 캐시를 삭제하여 데이터 일관성 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 캐시 키 패턴 상수들
    private static final String PRODUCT_DETAIL_KEY_PATTERN = "product:detail:%d";
    private static final String PRODUCT_METRICS_KEY_PATTERN = "product:metrics:%d";
    private static final String TOP_LIKED_PRODUCTS_KEY = "products:top_liked";
    private static final String PRODUCT_LIST_KEY_PATTERN = "products:list:*";

    /**
     * 상품 관련 캐시 무효화
     * 좋아요 변경, 상품 정보 변경 시 호출
     *
     * @param productId 상품 ID
     */
    public void evictProductCache(Long productId) {
        try {
            // 1. 상품 상세 정보 캐시 삭제
            String productDetailKey = String.format(PRODUCT_DETAIL_KEY_PATTERN, productId);
            redisTemplate.delete(productDetailKey);

            // 2. 상품 집계 정보 캐시 삭제 (좋아요 수 등)
            String productMetricsKey = String.format(PRODUCT_METRICS_KEY_PATTERN, productId);
            redisTemplate.delete(productMetricsKey);

            log.info("상품 캐시 삭제 완료 - productId: {}", productId);

        } catch (Exception e) {
            // 캐시 삭제 실패는 비즈니스 로직에 영향을 주지 않으므로 로그만 남김
            log.warn("상품 캐시 삭제 실패 - productId: {}, error: {}", productId, e.getMessage());
        }
    }

    /**
     * 인기 상품 랭킹 캐시 무효화
     * 좋아요 수 변경 시 호출하여 랭킹 재계산 유도
     */
    public void evictTopLikedProductsCache() {
        try {
            redisTemplate.delete(TOP_LIKED_PRODUCTS_KEY);
            log.info("인기 상품 랭킹 캐시 삭제 완료");

        } catch (Exception e) {
            log.warn("인기 상품 랭킹 캐시 삭제 실패 - error: {}", e.getMessage());
        }
    }

    /**
     * 상품 관련 모든 캐시 무효화
     * 대량 데이터 변경이나 시스템 동기화 시 사용
     *
     * @param productId 상품 ID
     */
    public void evictAllProductRelatedCache(Long productId) {
        evictProductCache(productId);
        evictTopLikedProductsCache();

        log.info("상품 관련 모든 캐시 삭제 완료 - productId: {}", productId);
    }

    /**
     * 상품 목록 캐시 무효화
     * 재고 소진/복구 시 호출하여 품절 상품 필터링이 적용된 목록 재생성 유도
     * 검색 조건별로 여러 캐시가 있을 수 있으므로 패턴 매칭으로 모두 삭제
     */
    public void evictProductListCache() {
        try {
            // products:list:* 패턴의 모든 키 삭제
            Set<String> keys = redisTemplate.keys(PRODUCT_LIST_KEY_PATTERN);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("상품 목록 캐시 삭제 완료 - 삭제된 키 개수: {}", keys.size());
            }
        } catch (Exception e) {
            log.warn("상품 목록 캐시 삭제 실패 - error: {}", e.getMessage());
        }
    }

}
