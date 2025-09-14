package com.loopers.interfaces;

import java.time.Instant;

/**
 * Kafka 이벤트 수신을 위한 EventEnvelope
 *
 * 목적:
 * - Producer(commerce-api)에서 전송한 EventEnvelope 구조와 동일
 * - Consumer(commerce-collector)에서 이벤트 메타데이터와 payload 분리 처리
 *
 * 구조 설명:
 * - eventType: 이벤트 타입 식별 (StockDecreasedEvent, LikeAddedEvent 등)
 * - eventId: Producer에서 생성한 고유 ID (멱등성 보장용)
 * - timestamp: Producer에서 이벤트 발행한 시간
 * - payload: 실제 이벤트 데이터 (StockDecreasedEvent 객체 등)
 *
 * JSON 예시:
 * {
 *   "eventType": "StockDecreasedEvent",
 *   "eventId": "evt_abc123456",
 *   "timestamp": "2024-01-01T10:00:00Z",
 *   "payload": {
 *     "productId": 123,
 *     "previousStock": 10,
 *     "currentStock": 8,
 *     "decreasedQuantity": 2,
 *     "reason": "ORDER",
 *     "occurredAt": "2024-01-01T10:00:00Z"
 *   }
 * }
 */
public record EventEnvelope(
    String eventType,     // 이벤트 타입 (Consumer 타입 식별용)
    String eventId,       // 고유 ID (멱등성 처리용, Producer가 생성)
    Instant timestamp,    // 이벤트 발행 시간 (Producer에서 설정)
    Object payload        // 실제 이벤트 데이터 (JSON 역직렬화 후 Object)
) {

    /**
     * 이벤트 객체를 EventEnvelope로 감싸는 팩토리 메서드
     *
     * @param event 실제 이벤트 객체 (RankingUpdateMessage 등)
     * @return EventEnvelope로 감싸진 이벤트
     */
    public static EventEnvelope wrap(Object event) {
        return new EventEnvelope(
                event.getClass().getSimpleName(),  // RankingUpdateMessage -> "RankingUpdateMessage"
                generateEventId(),                 // UUID 기반 고유 ID
                Instant.now(),                     // 현재 시간
                event                              // 실제 이벤트 객체
        );
    }

    /**
     * 멱등성 보장을 위한 고유 이벤트 ID 생성
     */
    private static String generateEventId() {
        return "evt_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

}
