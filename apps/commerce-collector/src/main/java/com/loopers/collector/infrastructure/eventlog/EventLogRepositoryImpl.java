package com.loopers.collector.infrastructure.eventlog;

import com.loopers.collector.domain.eventlog.EventLog;
import com.loopers.collector.domain.eventlog.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EventLogRepository 구현체
 * JPA를 통한 실제 데이터 접근 구현
 */
@Repository
@RequiredArgsConstructor
public class EventLogRepositoryImpl implements EventLogRepository {

    private final EventLogJpaRepository eventLogJpaRepository;

    @Override
    public EventLog save(EventLog eventLog) {
        return eventLogJpaRepository.save(eventLog);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return eventLogJpaRepository.existsByEventId(eventId);
    }

    @Override
    public Optional<EventLog> findByEventId(String eventId) {
        return eventLogJpaRepository.findByEventId(eventId);
    }

    @Override
    public List<EventLog> findByAggregateId(String aggregateId) {
        return eventLogJpaRepository.findByAggregateIdOrderByCreatedAtDesc(aggregateId);
    }

    @Override
    public List<EventLog> findRecentEventsByType(String eventType, Pageable pageable) {
        return eventLogJpaRepository.findRecentEventsByType(eventType, pageable);
    }

    @Override
    public Long countByEventTypeAndPeriod(String eventType, LocalDateTime startTime, LocalDateTime endTime) {
        return eventLogJpaRepository.countByEventTypeAndCreatedAtBetween(eventType, startTime, endTime);
    }

}
