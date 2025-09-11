package com.loopers.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.eventhandled.EventHandledService;
import com.loopers.application.eventhandler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ranking-events 토픽의 이벤트를 소비하는 Consumer
 *
 * 역할:
 * - ranking-events 토픽에서 RankingUpdateMessage 수신
 * - RankingEventHandler로 라우팅하여 Redis ZSET 업데이트
 * - EventHandler 패턴으로 랭킹 처리 분리
 *
 * 처리 대상:
 * - RankingUpdateMessage (좋아요 추가/취소에 따른 점수 변경)
 *
 * Redis ZSET 전략:
 * - Key: ranking:all:{yyyyMMdd} (일별 분리)
 * - TTL: 2일
 * - Score: actionType.getScoreChange() 값으로 증감
 *
 * EventEnvelope 패턴을 사용하여 표준화된 이벤트 처리:
 * - eventType으로 이벤트 타입 식별
 * - eventId로 멱등성 보장
 * - payload에서 실제 RankingUpdateMessage 데이터 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventConsumer {

    private final EventHandledService eventHandledService;
    private final ObjectMapper objectMapper;        // JSON 직렬화용
    private final List<EventHandler> eventHandlers; // Spring이 자동으로 모든 EventHandler 주입

    /**
     * ranking-events 토픽 메시지 처리
     *
     * 처리 흐름:
     * 1. EventEnvelope 파싱
     * 2. 적절한 EventHandler(RankingEventHandler) 찾아서 위임
     * 3. Manual ACK (Handler에서 멱등성 처리)
     *
     * 처리 예시:
     * - 좋아요 추가: productId=123, scoreChange=+0.2
     * - 좋아요 취소: productId=123, scoreChange=-0.2
     */
    @KafkaListener(
            topics = "ranking-events",
            groupId = "commerce-collector"
    )
    public void handleRankingEvent(
        ConsumerRecord<String, String> record,                 // ConsumerRecord로 직접 받아서 메타데이터와 메시지 모두 접근
        Acknowledgment ack                                      // 수동 ACK : 메시지 처리 완료 확인용
    ) {

        // ConsumerRecord에서 메타데이터와 메시지 추출
        String message = record.value();           // 실제 메시지 내용 (EventEnvelope JSON 문자열)
        String topic = record.topic();             // 토픽명 (ranking-events)
        int partition = record.partition();        // 파티션 번호
        long offset = record.offset();             // 오프셋(메시지 순번)
        String messageKey = record.key();          // 파티션 키(productId) - 같은 상품은 순서 보장

        try {

            // 1. EventEnvelope 파싱(Producer에서 감싼 구조 해제)
            EventEnvelope envelope;
            try {
                envelope = objectMapper.readValue(message, EventEnvelope.class);
            } catch (Exception parseException) {
                log.error("EventEnvelope 파싱 실패 - message: {}, error: {}",
                        message, parseException.getMessage());
                throw new RuntimeException("EventEnvelope 파싱 실패", parseException);
            }

            String eventType = envelope.eventType();
            Object payload = envelope.payload();    // 실제 RankingUpdateMessage 데이터
            log.info("Ranking 이벤트 수신 - eventType: {}, key: {}, topic: {}, partition: {}, offset: {}",
                    eventType, messageKey, topic, partition, offset);

            // 2. payload를 JSON으로 변환 (EventHandler들이 JSON 문자열을 받도록 설계)
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (Exception jsonException) {
                log.error("payload JSON 변환 실패 - eventType: {}, error: {}", eventType,
                        jsonException.getMessage());
                throw new RuntimeException("payload JSON 변환 실패", jsonException);
            }

            // 3. 적절한 EventHandler 찾아서 위임 (RankingEventHandler가 처리)
            EventHandler handler = findEventHandler(eventType);
            if (handler != null) {
                handler.handle(eventType, payloadJson, messageKey);
                log.debug("랭킹 이벤트 처리 완료 - eventType: {}, handler: {}",
                        eventType, handler.getClass().getSimpleName());
            } else {
                // 알 수 없는 이벤트는 단순히 로그만 남김 (현재는 RankingUpdateMessage만 처리)
                log.warn("처리할 수 있는 Handler가 없음 - eventType: {}", eventType);
            }

            // 4. 메시지 처리 완료 확인 (Manual ACK)
            ack.acknowledge();
            log.info("Ranking 이벤트 처리 완료 - eventType: {}, key: {}", eventType, messageKey);

        } catch (Exception e) {
            log.error("Ranking 이벤트 처리 실패 - message: {}, error: {}", message, e.getMessage(), e);

            throw e;
        }
    }

    /**
     * 이벤트 타입에 맞는 EventHandler 찾기
     *
     * @param eventType 이벤트 타입 (예: "RankingUpdateMessage")
     * @return 처리 가능한 Handler, 없으면 null
     */
    private EventHandler findEventHandler(String eventType) {
        return eventHandlers.stream()
            .filter(handler -> handler.canHandle(eventType))
            .findFirst()
            .orElse(null);
    }

}
