package com.loopers.consumer.handler;

import com.loopers.collector.application.cache.CacheEvictService;
import com.loopers.collector.application.eventlog.EventLogService;
import com.loopers.collector.application.metrics.MetricsService;
import com.loopers.collector.common.EventDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 좋아요 관련 이벤트 처리 Handler
 *
 * 담당 이벤트:
 * - LikeAddedEvent: 좋아요 추가 시 처리
 * - LikeRemovedEvent: 좋아요 취소 시 처리
 *
 * 처리 로직 (3종 처리):
 * 1. Audit Log: 모든 좋아요 이벤트를 event_log 테이블에 저장
 * 2. Cache Evict: 상품 캐시 + 인기 상품 랭킹 캐시 무효화
 * 3. Metrics: product_metrics 테이블의 좋아요 수 증감
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler implements EventHandler {

    private final MetricsService metricsService;
    private final EventLogService eventLogService;
    private final CacheEvictService cacheEvictService;
    private final EventDeserializer eventDeserializer;

    /**
     * 처리 가능한 이벤트 타입들 반환
     */
    @Override
    public String[] getSupportedEventTypes() {
        return new String[]{"LikeAddedEvent", "LikeRemovedEvent"};
    }

    /**
     * 좋아요 이벤트 처리 메인 로직
     * 이벤트 타입에 따라 증가/감소 분기 처리
     */
    @Override
    public void handle(String eventType, String payloadJson, String messageKey) {
        try {
            Long productId = Long.parseLong(messageKey);

            switch (eventType) {
                case "LikeAddedEvent" -> handleLikeAdded(payloadJson, productId);
                case "LikeRemovedEvent" -> handleLikeRemoved(payloadJson, productId);
                default -> throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + eventType);
            }

            log.info("좋아요 이벤트 처리 완료 - eventType: {}, productId: {}", eventType, productId);

        } catch (Exception e) {
            log.error("좋아요 이벤트 처리 실패 - eventType: {}, messageKey: {}, error: {}",
                    eventType, messageKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 좋아요 추가 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + Metrics Update
     */
    private void handleLikeAdded(String payloadJson, Long productId) {
        try {
            // JSON을 이벤트 객체로 변환 (현재는 Object로 처리)
            Object likeAddedEvent = eventDeserializer.deserialize(payloadJson, Object.class);

            // 1. 감사 로그 저장 - 모든 좋아요 이벤트를 추적 가능하게 기록
            eventLogService.saveEventLog(likeAddedEvent, "LikeAddedEvent", productId.toString(), "PRODUCT");

            // 2. 상품 관련 캐시 무효화 - 사용자가 최신 좋아요 수를 볼 수 있도록
            cacheEvictService.evictProductCache(productId);              // 해당 상품 캐시 삭제
            cacheEvictService.evictTopLikedProductsCache();              // 인기 상품 랭킹 캐시도 무효화

            // 3. 좋아요 수 증가 - 집계 테이블에 실시간 반영
            metricsService.increaseLikeCount(productId);

            log.debug("좋아요 추가 처리 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("좋아요 추가 처리 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 좋아요 취소 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + Metrics Update
     */
    private void handleLikeRemoved(String payloadJson, Long productId) {
        try {
            // JSON을 이벤트 객체로 변환
            Object likeRemovedEvent = eventDeserializer.deserialize(payloadJson, Object.class);

            // 1. 감사 로그 저장
            eventLogService.saveEventLog(likeRemovedEvent, "LikeRemovedEvent", productId.toString(), "PRODUCT");

            // 2. 상품 관련 캐시 무효화
            cacheEvictService.evictProductCache(productId);
            cacheEvictService.evictTopLikedProductsCache(); // 랭킹 변동으로 인한 캐시 무효화

            // 3. 좋아요 수 감소 - 0 이하로는 내려가지 않도록 MetricsService에서 처리
            metricsService.decreaseLikeCount(productId);

            log.debug("좋아요 취소 처리 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("좋아요 취소 처리 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

}
