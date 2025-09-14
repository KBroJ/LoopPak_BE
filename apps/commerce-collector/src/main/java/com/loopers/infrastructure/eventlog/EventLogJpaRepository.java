package com.loopers.infrastructure.eventlog;

import com.loopers.domain.eventlog.EventLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventLogJpaRepository extends JpaRepository<EventLog, Long> {

    boolean existsByEventId(String eventId);

    Optional<EventLog> findByEventId(String eventId);

    @Query("SELECT el FROM EventLog el WHERE el.aggregateId = :aggregateId ORDER BY el.createdAt DESC")
    List<EventLog> findByAggregateIdOrderByCreatedAtDesc(@Param("aggregateId") String aggregateId);

    @Query("SELECT el FROM EventLog el WHERE el.eventType = :eventType ORDER BY el.createdAt DESC")
    List<EventLog> findRecentEventsByType(@Param("eventType") String eventType, Pageable pageable);

    @Query("SELECT COUNT(el) FROM EventLog el WHERE el.eventType = :eventType AND el.createdAt BETWEEN :startTime AND :endTime")
    Long countByEventTypeAndCreatedAtBetween(
        @Param("eventType") String eventType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

}
