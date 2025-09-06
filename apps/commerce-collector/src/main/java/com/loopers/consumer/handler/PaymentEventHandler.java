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
 * Payment 관련 이벤트를 처리하는 Handler
 *
 * 처리 이벤트:
 * - PaymentSuccessEvent: 결제 성공 시 처리
 * - PaymentFailureEvent: 결제 실패 시 처리
 *
 * 처리 내용:
 * 1. 감사 로그: event_log 테이블에 저장
 * 2. 캐시 무효화: 주문 관련 캐시 삭제 (필요시)
 * 3. 메트릭스: 결제 성공/실패 집계 데이터 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler implements EventHandler {

    private final EventLogService eventLogService;
    private final CacheEvictService cacheEvictService;
    private final EventDeserializer eventDeserializer;
    private final ObjectMapper objectMapper;

    /**
     * 처리 가능한 이벤트 타입들 반환
     */
    @Override
    public String[] getSupportedEventTypes() {
        return new String[]{"PaymentSuccessEvent", "PaymentFailureEvent"};
    }

    @Override
    public boolean canHandle(String eventType) {
        return "PaymentSuccessEvent".equals(eventType) ||
                "PaymentFailureEvent".equals(eventType);
    }

    /**
     * 결제 이벤트 처리 메인 로직
     */
    @Override
    public void handle(String eventType, String payloadJson, String messageKey) {
        try {
            Long orderId = Long.parseLong(messageKey);

            switch (eventType) {
                case "PaymentSuccessEvent" -> handlePaymentSuccess(payloadJson, orderId);
                case "PaymentFailureEvent" -> handlePaymentFailure(payloadJson, orderId);
                default -> throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + eventType);
            }

            log.info("결제 이벤트 처리 완료 - eventType: {}, orderId: {}", eventType, orderId);

        } catch (Exception e) {
            log.error("결제 이벤트 처리 실패 - eventType: {}, messageKey: {}, error: {}",
                    eventType, messageKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 결제 성공 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + (Metrics는 필요시)
     */
    private void handlePaymentSuccess(String payloadJson, Long orderId) {
        try {
            // JSON을 PaymentSuccessEvent 객체로 변환 (현재는 Object로 처리)
            Object paymentSuccessEvent = eventDeserializer.deserialize(payloadJson, Object.class);

            // 1. 감사 로그 저장 - 결제 성공 이력 관리
            eventLogService.saveEventLog(paymentSuccessEvent, "PaymentSuccessEvent",
                    orderId.toString(), "ORDER");

            // 2. 캐시 무효화 - 필요시 결제/주문 관련 캐시 처리
            // 현재는 결제별 캐시가 없으므로 Skip
            // cacheEvictService.evictPaymentCache(orderId);

            // 3. 비즈니스 정보 추출 (JsonNode 방식)
            try {
                JsonNode jsonNode = objectMapper.readTree(payloadJson);
                Long userId = jsonNode.get("userId").asLong();
                String paymentType = jsonNode.get("paymentType").asText();
                Long amount = jsonNode.get("amount").asLong();
                String transactionKey = jsonNode.get("transactionKey").asText();

                log.info("결제 성공 처리 완료 - orderId: {}, userId: {}, paymentType: {}, amount: {}, transactionKey: {}",
                                                orderId, userId, paymentType, amount, transactionKey);

                // TODO: 결제 집계가 필요하면 여기서 처리
                // 4. 집계 처리 (향후 확장 가능)
                // - 사용자별 결제 성공 수 증가
                // - 일별 결제 성공 매출 집계
                // - 결제 방식별 성공 통계 등

            } catch (Exception e) {
                log.warn("결제 성공 정보 추출 실패 - orderId: {}, payloadJson: {}, error: {}",
                        orderId, payloadJson, e.getMessage());
            }

            log.debug("결제 성공 처리 완료 - orderId: {}", orderId);

        } catch (Exception e) {
            log.error("결제 성공 처리 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 결제 실패 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + (Metrics는 필요시)
     */
    private void handlePaymentFailure(String payloadJson, Long orderId) {
        try {
            // JSON을 PaymentFailureEvent 객체로 변환 (현재는 Object로 처리)
            Object paymentFailureEvent = eventDeserializer.deserialize(payloadJson, Object.class);

            // 1. 감사 로그 저장 - 결제 실패 이력 관리
            eventLogService.saveEventLog(paymentFailureEvent, "PaymentFailureEvent",
                    orderId.toString(), "ORDER");

            // 2. 캐시 무효화 - 필요시 결제/주문 관련 캐시 처리
            // 현재는 결제별 캐시가 없으므로 Skip
            // cacheEvictService.evictPaymentCache(orderId);

            // 3. 비즈니스 정보 추출 (JsonNode 방식)
            try {
                JsonNode jsonNode = objectMapper.readTree(payloadJson);
                Long userId = jsonNode.get("userId").asLong();
                String paymentType = jsonNode.get("paymentType").asText();
                Long amount = jsonNode.get("amount").asLong();
                String message = jsonNode.get("message").asText();

                log.info("결제 실패 처리 완료 - orderId: {}, userId: {}, paymentType: {}, amount: {}, message: {}",
                                                orderId, userId, paymentType, amount, message);

                // TODO: 결제 실패 집계가 필요하면 여기서 처리
                // 4. 집계 처리 (향후 확장 가능)
                // - 사용자별 결제 실패 수 증가
                // - 일별 결제 실패 통계
                // - 실패 사유별 분석 등

            } catch (Exception e) {
                log.warn("결제 실패 정보 추출 실패 - orderId: {}, payloadJson: {}, error: {}",
                        orderId, payloadJson, e.getMessage());
            }

            log.debug("결제 실패 처리 완료 - orderId: {}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

}
