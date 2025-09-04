package com.loopers.collector.infrastructure.eventhandled;

import com.loopers.collector.domain.eventhandled.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * EventHandled 도메인의 JPA Repository 구현체
 *
 * 주요 특징:
 * - JpaRepository<EventHandled, String>: EventHandled 엔티티, PK 타입은 String
 * - existsByEventId는 JPA가 메서드명으로 자동 쿼리 생성
 * - save는 JpaRepository에서 제공하는 기본 메서드
 */
public interface EventHandledJpaRepository extends JpaRepository<EventHandled, String> {

    /**
     * eventId로 존재 여부 확인
     * SELECT COUNT(*) > 0 FROM event_handled WHERE event_id = ?
     *
     * @param eventId 검색할 이벤트 ID
     * @return 존재 여부 (true/false)
     */
    boolean existsByEventId(String eventId);

}
