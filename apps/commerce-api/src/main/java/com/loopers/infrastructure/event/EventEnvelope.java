package com.loopers.infrastructure.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka 이벤트 전송을 위한 Envelope 패턴 구현
 *
 * 목적:
 * - 이벤트 메타데이터(타입, ID, 타임스탬프)와 실제 데이터 분리
 * - eventType 필드로 Consumer에서 쉬운 타입 감지
 * - 멱등성을 위한 고유 eventId 제공
 * - 표준화된 이벤트 발행 형태
 *
 * JSON 형태:
 * {
 *   "eventType": "StockDecreasedEvent",
 *   "eventId": "uuid-12345",
 *   "timestamp": "2024-01-01T10:00:00Z",
 *   "payload": { 실제 이벤트 데이터 }
 * }
 */
public record EventEnvelope(
    String eventType,     // 이벤트 타입 (Consumer에서 타입 감지용)
    String eventId,       // 고유 ID (멱등성 처리용)
    Instant timestamp,    // 이벤트 발행 시간
    Object payload        // 실제 이벤트 데이터 (StockDecreasedEvent 등)
) {

    /**
     * 이벤트 객체를 EventEnvelope로 감싸는 팩토리 메서드
     *
     * @param event 실제 이벤트 객체 (StockDecreasedEvent, LikeAddedEvent 등)
     * @return EventEnvelope로 감싸진 이벤트
     */
    public static EventEnvelope wrap(Object event) {
        return new EventEnvelope(
            event.getClass().getSimpleName(),  // StockDecreasedEvent -> "StockDecreasedEvent"
            generateEventId(),                 // UUID 기반 고유 ID
            Instant.now(),                     // 현재 시간
            event                              // 실제 이벤트 객체
        );
    }

    /**
     * 멱등성 보장을 위한 고유 이벤트 ID 생성
     * 형태: "evt_" + UUID (consumer에서 쉽게 식별)
     */
    private static String generateEventId() {
        return "evt_" + UUID.randomUUID().toString().replace("-", "");
    }

}
