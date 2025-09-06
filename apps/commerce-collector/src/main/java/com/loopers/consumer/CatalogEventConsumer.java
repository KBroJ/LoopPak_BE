package com.loopers.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.collector.application.eventhandled.EventHandledService;
import com.loopers.consumer.handler.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * catalog-events 토픽의 이벤트를 소비하는 Consumer
 *
 * 역할:
 * - Kafka 메시지 수신 및 EventEnvelope 파싱
 * - 적절한 EventHandler로 라우팅
 * - 멱등성 처리 및 Manual ACK
 * - 단일 책임 원칙 적용 (라우팅만 담당)
 * - EventHandler 패턴으로 도메인별 처리 분리
 *
 * EventEnvelope 패턴을 사용하여 표준화된 이벤트 처리:
 * - eventType으로 이벤트 타입 식별
 * - eventId로 멱등성 보장
 * - payload에서 실제 이벤트 데이터 추출
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {

    private final EventHandledService eventHandledService;
    private final ObjectMapper objectMapper;        // JSON 직렬화용
    private final List<EventHandler> eventHandlers; // Spring이 자동으로 모든 EventHandler 주입

    /**
     * catalog-events 토픽 메시지 처리
     *
     * 처리 흐름:
     * 1. EventEnvelope 파싱
     * 2. 멱등성 체크 (중복 처리 방지)
     * 3. 적절한 EventHandler 찾아서 위임
     * 4. 처리 완료 기록 및 ACK
     */
    @KafkaListener(
            topics = "catalog-events",
            groupId = "commerce-collector",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCatalogEvent(
            @Payload String message,                                // 메시지 내용(@Payload : 실제 메시지 내용, EventEnvelope JSON 문자열)
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,      // 토픽명
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, // 파티션 번호
            @Header(KafkaHeaders.OFFSET) long offset,               // 오프셋(메시지 순번)
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,   // 파티션 키(productId)
            Acknowledgment ack                                      // 수동 ACK : 메시지 처리 완료 확인용
    ) {

        try {

            // EventEnvelope 파싱(Producer에서 감싼 구조 해제)
            EventEnvelope envelope;
            try {
                envelope = objectMapper.readValue(message, EventEnvelope.class);
            } catch (Exception parseException) {
                log.error("EventEnvelope 파싱 실패 - message: {}, error: {}",
                        message, parseException.getMessage());
                throw new RuntimeException("EventEnvelope 파싱 실패", parseException);
            }

            String eventType = envelope.eventType();
            String eventId = envelope.eventId();    // Producer에서 생성한 고유 ID 사용(멱등성 처리용 고유 키)
            Object payload = envelope.payload();    // 실제 이벤트 데이터

            log.info("Catalog 이벤트 수신 - eventType: {}, eventId: {}, key: {}", eventType, eventId, messageKey);

            // 중복 처리 확인 (멱등성 체크)
            if (eventHandledService.isAlreadyHandled(eventId)) {
                log.info("이미 처리된 이벤트 - eventId: {}", eventId);
                ack.acknowledge(); // 중복이면 바로 ACK하고 종료
                return;
            }

            // 3. payload를 JSON으로 변환 (EventHandler들이 JSON 문자열을 받도록 설계)
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (Exception jsonException) {
                log.error("payload JSON 변환 실패 - eventType: {}, eventId: {}, error: {}",
                        eventType, eventId, jsonException.getMessage());
                throw new RuntimeException("payload JSON 변환 실패", jsonException);
            }

            // 4. 적절한 EventHandler 찾아서 위임
            EventHandler handler = findEventHandler(eventType);
            if (handler != null) {
                handler.handle(eventType, payloadJson, messageKey);
                log.debug("이벤트 처리 완료 - eventType: {}, handler: {}",
                        eventType, handler.getClass().getSimpleName());
            } else {
                // 알 수 없는 이벤트는 단순히 로그만 남김
                log.warn("처리할 수 있는 Handler가 없음 - eventType: {}", eventType);
            }

            // 5. 처리 완료 기록 (멱등성 보장)
            eventHandledService.markAsHandled(eventId, eventType, messageKey);

            // 6. 메시지 처리 완료 확인 (Manual ACK)
            ack.acknowledge();
            log.info("Catalog 이벤트 처리 완료 - eventType: {}, key: {}", eventType, messageKey);

        } catch (Exception e) {
            log.error("Catalog 이벤트 처리 실패 - message: {}, error: {}", message, e.getMessage(), e);

            // TODO: DLQ(Dead Letter Queue) 처리 고려
            // 현재는 예외 발생 시 메시지 재시도됨 (Kafka Consumer 기본 동작)
            throw e;
        }
    }

    /**
     * 이벤트 타입에 맞는 EventHandler 찾기
     *
     * @param eventType 이벤트 타입 (예: "LikeAddedEvent")
     * @return 처리 가능한 Handler, 없으면 null
     */
    private EventHandler findEventHandler(String eventType) {
        return eventHandlers.stream()
                .filter(handler -> handler.canHandle(eventType))
                .findFirst()
                .orElse(null);
    }

}
