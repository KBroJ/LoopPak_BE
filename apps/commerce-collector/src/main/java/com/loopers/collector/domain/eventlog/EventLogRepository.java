package com.loopers.collector.domain.eventlog;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EventLog 도메인 Repository 인터페이스
 * 도메인 관점에서의 이벤트 로그 저장소 추상화
 */
public interface EventLogRepository {

    /**
     * 이벤트 로그 저장
     */
    EventLog save(EventLog eventLog);

    /**
     * 멱등성 보장을 위한 이벤트 ID 존재 여부 확인
     */
    boolean existsByEventId(String eventId);

    /**
     * 이벤트 ID로 이벤트 로그 조회
     */
    Optional<EventLog> findByEventId(String eventId);

    /**
     * 특정 집계 ID의 이벤트 로그 목록 조회 (최신순)
     */
    List<EventLog> findByAggregateId(String aggregateId);

    /**
     * 특정 이벤트 타입의 최근 이벤트 목록 조회
     */
    List<EventLog> findRecentEventsByType(String eventType, Pageable pageable);

    /**
     * 특정 기간 내 이벤트 개수 조회
     */
    Long countByEventTypeAndPeriod(String eventType, LocalDateTime startTime, LocalDateTime endTime);

}
