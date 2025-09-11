package com.loopers.application.eventhandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.cache.CacheEvictService;
import com.loopers.application.eventhandled.EventHandledService;
import com.loopers.application.eventlog.EventLogService;
import com.loopers.common.EventDeserializer;
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
    private final EventHandledService eventHandledService;

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

            // JSON에서 eventId 추출하여 멱등성 체크
            String eventId = extractEventId(payloadJson);
            if (eventHandledService.isAlreadyHandled(eventId)) {
                log.info("이미 처리된 주문 이벤트 - eventId: {}", eventId);
                return; // 중복 이벤트면 바로 종료
            }

            Long orderId = Long.parseLong(messageKey);

            switch (eventType) {
                case "OrderCreatedEvent" -> handleOrderCreated(payloadJson, orderId);
                default -> throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + eventType);
            }

            // 처리 완료 기록 (멱등성 보장)
            eventHandledService.markAsHandled(eventId, eventType, messageKey);

            log.info("주문 이벤트 처리 완료 - eventType: {}, orderId: {}", eventType, orderId);

        } catch (Exception e) {
            log.error("주문 이벤트 처리 실패 - eventType: {}, messageKey: {}, error: {}",
                    eventType, messageKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * JSON payload에서 eventId 추출
     * Consumer에서 전달받은 payload JSON에서 eventId를 찾아 반환
     */
    private String extractEventId(String payloadJson) {
        try {
            JsonNode payloadNode = objectMapper.readTree(payloadJson);
            JsonNode eventIdNode = payloadNode.get("eventId");

            if (eventIdNode == null || eventIdNode.isNull()) {
                throw new IllegalArgumentException("payload에 eventId가 없습니다: " + payloadJson);
            }

            return eventIdNode.asText();

        } catch (Exception e) {
            log.error("eventId 추출 실패 - payloadJson: {}, error: {}", payloadJson, e.getMessage());
            throw new RuntimeException("eventId 추출 실패", e);
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
