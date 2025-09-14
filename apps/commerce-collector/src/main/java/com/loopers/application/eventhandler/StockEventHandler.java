package com.loopers.application.eventhandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.cache.CacheEvictService;
import com.loopers.application.eventhandled.EventHandledService;
import com.loopers.application.eventlog.EventLogService;
import com.loopers.application.metrics.MetricsService;
import com.loopers.common.EventDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 재고 관련 이벤트 처리 Handler
 *
 * 담당 이벤트:
 * - StockDecreasedEvent: 재고 감소 시 처리 (주로 주문으로 인한 재고 차감)
 * - StockIncreasedEvent: 재고 증가 시 처리 (주로 재입고)
 *
 * 처리 로직 (3종 처리):
 * 1. Audit Log: 모든 재고 변동을 event_log 테이블에 저장
 * 2. Cache Evict: 상품 캐시 무효화 (재고 정보 변경으로 인한)
 * 3. Metrics: product_metrics 테이블의 판매량 증가 (재고 감소 시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventHandler implements EventHandler {

    private final EventLogService eventLogService;
    private final CacheEvictService cacheEvictService;
    private final MetricsService metricsService;
    private final EventDeserializer eventDeserializer;
    private final ObjectMapper objectMapper;
    private final EventHandledService eventHandledService;

    /**
     * 처리 가능한 이벤트 타입들 반환
     */
    @Override
    public String[] getSupportedEventTypes() {
        return new String[]{"StockDecreasedEvent", "StockIncreasedEvent"};
    }

    /**
     * 재고 이벤트 처리 메인 로직
     * 이벤트 타입에 따라 감소/증가 분기 처리
     */
    @Override
    public void handle(String eventType, String payloadJson, String messageKey) {
        try {

            // JSON에서 eventId 추출하여 멱등성 체크
            String eventId = extractEventId(payloadJson);
            if (eventHandledService.isAlreadyHandled(eventId)) {
                log.info("이미 처리된 재고 이벤트 - eventId: {}", eventId);
                return; // 중복 이벤트면 바로 종료
            }

            Long productId = Long.parseLong(messageKey);

            switch (eventType) {
                case "StockDecreasedEvent" -> handleStockDecreased(payloadJson, productId);
                case "StockIncreasedEvent" -> handleStockIncreased(payloadJson, productId);
                default -> throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + eventType);
            }

            // 처리 완료 기록 (멱등성 보장)
            eventHandledService.markAsHandled(eventId, eventType, messageKey);

            log.info("재고 이벤트 처리 완료 - eventType: {}, productId: {}", eventType, productId);

        } catch (Exception e) {
            log.error("재고 이벤트 처리 실패 - eventType: {}, messageKey: {}, error: {}",
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
     * 재고 감소 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + Metrics Update
     */
    private void handleStockDecreased(String payloadJson, Long productId) {
        try {
            // JSON을 StockDecreasedEvent 객체로 변환
            Object stockDecreasedEvent = eventDeserializer.deserialize(payloadJson, Object.class);

            // 1. 감사 로그 저장 - 재고 변동 추적을 위한 이력 관리
            eventLogService.saveEventLog(stockDecreasedEvent, "StockDecreasedEvent",
                    productId.toString(), "PRODUCT");

            // 2. 상품 관련 캐시 무효화 - 재고 변경으로 인한 상품 정보 갱신
            cacheEvictService.evictProductCache(productId);     // 상품 상세 캐시 삭제

            // 3. 판매량 집계 - 재고 감소는 일반적으로 판매를 의미
            // TODO: 향후 StockDecreasedEvent에서 reason 필드를 확인하여
            //       주문으로 인한 감소인지, 다른 사유인지 구분하여 처리
            metricsService.increaseSalesCount(productId);       // 기본 1개씩 증가

            // 4. 재고 소진 확인 및 추가 캐시 처리
            try {
                JsonNode jsonNode = objectMapper.readTree(payloadJson);
                int currentStock = jsonNode.get("currentStock").asInt();

                if (currentStock == 0) {
                    log.info("재고 소진 감지 - 상품 목록 캐시 무효화 진행, productId: {}", productId);
                    // 품절 시 상품 목록 캐시도 무효화 (품절 상품 필터링 때문)
                    cacheEvictService.evictProductListCache();

                    // TODO: 향후 품절 알림 이벤트 발행 등 추가 처리 고려
                    // publishOutOfStockEvent(productId);
                }
            } catch (Exception e) {
                log.warn("재고 소진 확인 처리 실패 - productId: {}, payloadJson: {}, error: {}",
                        productId, payloadJson, e.getMessage());
                // 캐시 처리 실패는 비즈니스 로직에 영향 주지 않으므로 예외를 다시 던지지 않음
            }

            log.debug("재고 감소 처리 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("재고 감소 처리 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 재고 증가 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + (Metrics는 필요시)
     */
    private void handleStockIncreased(String payloadJson, Long productId) {
        try {
            // JSON을 StockIncreasedEvent 객체로 변환
            Object stockIncreasedEvent = eventDeserializer.deserialize(payloadJson, Object.class);

            // 1. 감사 로그 저장 - 재입고 등의 재고 증가 이력 관리
            eventLogService.saveEventLog(stockIncreasedEvent, "StockIncreasedEvent",
                    productId.toString(), "PRODUCT");

            // 2. 상품 관련 캐시 무효화 - 재고 증가로 인한 상품 정보 갱신
            cacheEvictService.evictProductCache(productId);     // 상품 상세 캐시 삭제

            // 3. 집계 업데이트 - 재고 증가는 일반적으로 별도 집계 불필요
            // 단, 특별한 요구사항이 있다면 여기서 처리 (예: 재입고 횟수 등)

            // 4. 품절 복구 처리
            try {
                JsonNode jsonNode = objectMapper.readTree(payloadJson);
                int previousStock = jsonNode.get("previousStock").asInt();
                int currentStock = jsonNode.get("currentStock").asInt();

                if (previousStock == 0 && currentStock > 0) {
                    log.info("품절 복구 감지 - 상품 목록 캐시 무효화 진행, productId: {}", productId);
                    // 품절에서 복구 시 상품 목록 캐시 무효화 (재진열을 위해)
                    cacheEvictService.evictProductListCache();

                    // TODO: 향후 재입고 알림 이벤트 발행 등 추가 처리 고려
                    // publishRestockEvent(productId);
                }
            } catch (Exception e) {
                log.warn("품절 복구 확인 처리 실패 - productId: {}, payloadJson: {}, error: {}",
                        productId, payloadJson, e.getMessage());
            }

            log.debug("재고 증가 처리 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("재고 증가 처리 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

}
