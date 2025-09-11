package com.loopers.domain.eventlog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이벤트 감사 로그 엔티티
 * 모든 도메인 이벤트를 저장하여 추적 가능성과 디버깅을 지원
 */
@Entity
@Table(
    name = "event_log",
    indexes = @Index(name = "idx_event_id", columnList = "eventId", unique = true))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 멱등성 보장을 위한 고유 이벤트 ID
     * 형식: "이벤트타입_집계ID_사용자ID_타임스탬프_해시"
     * 예: "LikeAdded_123_456_20241203_001"
     */
    @Column(name = "event_id", unique = true, nullable = false, length = 255)
    private String eventId;

    /**
     * 이벤트 타입 (클래스명)
     * 예: "LikeAddedEvent", "OrderCreatedEvent"
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * 집계 루트 ID (productId, orderId 등)
     * 이벤트가 영향을 주는 주요 엔티티의 ID
     */
    @Column(name = "aggregate_id", length = 255)
    private String aggregateId;

    /**
     * 집계 루트 타입
     * 예: "PRODUCT", "ORDER", "USER"
     */
    @Column(name = "aggregate_type", length = 50)
    private String aggregateType;

    /**
     * 이벤트 전체 내용 (JSON 형태)
     * 원본 이벤트의 모든 데이터를 보존
     */
    @Column(name = "event_data", columnDefinition = "JSON")
    private String eventData;

    /**
     * 이벤트 처리 완료 시간
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * 이벤트 로그 생성 시간
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.processedAt == null) {
            this.processedAt = LocalDateTime.now();
        }
    }

    /**
     * EventLog 생성을 위한 정적 팩토리 메서드
     */
    public static EventLog of(String eventId, String eventType, String aggregateId,
                              String aggregateType, String eventData) {
        return EventLog.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventData(eventData)
                .build();
    }

}
