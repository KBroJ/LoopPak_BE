package com.loopers.consumer.handler;

/**
 * 이벤트 처리를 위한 공통 인터페이스
 *
 * 목적:
 * - 각 도메인별 이벤트 처리 로직을 분리
 * - 단일 책임 원칙(SRP) 적용
 * - 새로운 이벤트 타입 추가 시 확장성 보장
 *
 * 구현체들:
 * - LikeEventHandler: 좋아요 관련 이벤트 처리
 * - StockEventHandler: 재고 관련 이벤트 처리
 * - 향후 OrderEventHandler, PaymentEventHandler 등 확장 가능
 */
public interface EventHandler {

    /**
     * 처리 가능한 이벤트 타입들을 반환
     *
     * @return 이벤트 타입 배열 (예: ["LikeAddedEvent", "LikeRemovedEvent"])
     */
    String[] getSupportedEventTypes();

    /**
     * 이벤트 처리 메인 로직
     *
     * @param eventType 이벤트 타입 (예: "LikeAddedEvent")
     * @param payloadJson 실제 이벤트 데이터 JSON
     * @param messageKey 파티션 키 (보통 productId)
     */
    void handle(String eventType, String payloadJson, String messageKey);

    /**
     * 특정 이벤트 타입을 처리할 수 있는지 확인
     *
     * @param eventType 이벤트 타입
     * @return 처리 가능 여부
     */
    default boolean canHandle(String eventType) {
        for (String supportedType : getSupportedEventTypes()) {
            if (supportedType.equals(eventType)) {
                return true;
            }
        }
        return false;
    }

}
