package com.loopers.domain.eventhandled;

/**
 * 이벤트 중복 처리 방지를 위한 Repository 인터페이스
 *
 * 역할:
 * - 특정 eventId가 이미 처리되었는지 확인 (멱등성 체크)
 * - 처리 완료된 이벤트 기록 저장
 *
 */
public interface EventHandledRepository {

    /**
     * 특정 이벤트가 이미 처리되었는지 확인
     *
     * @param eventId Kafka 메시지 고유 ID (예: "catalog-events-0-123")
     * @return 처리 여부
     *         - true: 이미 처리됨 (중복 메시지, Skip 처리)
     *         - false: 미처리 (신규 메시지, 비즈니스 로직 실행)
     */
    boolean existsByEventId(String eventId);

    /**
     * 이벤트 처리 완료 기록 저장
     *
     * @param eventHandled 처리 완료 기록 엔티티
     * @return 저장된 엔티티
     */
    EventHandled save(EventHandled eventHandled);

}
