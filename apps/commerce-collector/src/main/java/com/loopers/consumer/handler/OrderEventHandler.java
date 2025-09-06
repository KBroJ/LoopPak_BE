package com.loopers.consumer.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.collector.application.cache.CacheEvictService;
import com.loopers.collector.application.eventlog.EventLogService;
import com.loopers.collector.common.EventDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주문 관련 이벤트 처리 Handler
 *
 * 담당 이벤트:
 * - OrderCreatedEvent: 주문 생성 시 처리
 *
 * 처리 로직 (3종 처리):
 * 1. Audit Log: 모든 주문 생성을 event_log 테이블에 저장
 * 2. Cache Evict: 주문 관련 캐시 무효화 (필요시)
 * 3. Metrics: order_metrics 테이블 집계 (주문 수, 매출 등)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler implements EventHandler {

    private final EventLogService eventLogService;
    private final CacheEvictService cacheEvictService;
    // private final MetricsService metricsService; // 향후 주문 집계가 필요하면 추가
    private final EventDeserializer eventDeserializer;
    private final ObjectMapper objectMapper;

    /**
     * 처리 가능한 이벤트 타입들 반환
     */
    @Override
    public String[] getSupportedEventTypes() {
        return new String[]{"OrderCreatedEvent"};
    }

    /**
     * 주문 이벤트 처리 메인 로직
     */
    @Override
    public void handle(String eventType, String payloadJson, String messageKey) {
        try {
            Long orderId = Long.parseLong(messageKey);

            switch (eventType) {
                case "OrderCreatedEvent" -> handleOrderCreated(payloadJson, orderId);
                default -> throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + eventType);
            }

            log.info("주문 이벤트 처리 완료 - eventType: {}, orderId: {}", eventType, orderId);

        } catch (Exception e) {
            log.error("주문 이벤트 처리 실패 - eventType: {}, messageKey: {}, error: {}",
                    eventType, messageKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 주문 생성 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + (Metrics는 필요시)
     */
    private void handleOrderCreated(String payloadJson, Long orderId) {
        try {
            // JSON을 OrderCreatedEvent 객체로 변환 (현재는 Object로 처리)
            Object orderCreatedEvent = eventDeserializer.deserialize(payloadJson, Object.class);

            // 1. 감사 로그 저장 - 주문 생성 이력 관리
            eventLogService.saveEventLog(orderCreatedEvent, "OrderCreatedEvent",
                    orderId.toString(), "ORDER");

            // 2. 캐시 무효화 - 필요시 주문 관련 캐시 처리
            // 현재는 주문별 캐시가 없으므로 Skip
            // cacheEvictService.evictOrderCache(orderId);

            // 3. 비즈니스 정보 추출 (JsonNode 방식)
            try {
                JsonNode jsonNode = objectMapper.readTree(payloadJson);
                Long userId = jsonNode.get("userId").asLong();
                long finalPrice = jsonNode.get("finalPrice").asLong();
                String paymentType = jsonNode.get("paymentType").asText();

                log.info("주문 생성 처리 완료 - orderId: {}, userId: {}, finalPrice: {}, paymentType: {}",
                        orderId, userId, finalPrice, paymentType);

                // TODO: 주문 집계가 필요하면 여기서 처리
                // 4. 집계 처리 (향후 확장 가능)
                // - 사용자별 주문 수 증가
                // - 일별 매출 집계
                // - 결제 방식별 통계 등

            } catch (Exception e) {
                log.warn("주문 정보 추출 실패 - orderId: {}, payloadJson: {}, error: {}",
                        orderId, payloadJson, e.getMessage());
            }

            log.debug("주문 생성 처리 완료 - orderId: {}", orderId);

        } catch (Exception e) {
            log.error("주문 생성 처리 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

}
