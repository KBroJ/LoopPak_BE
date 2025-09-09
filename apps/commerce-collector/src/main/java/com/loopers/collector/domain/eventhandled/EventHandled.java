package com.loopers.collector.domain.eventhandled;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이벤트 중복 처리 방지를 위한 멱등성 테이블
 * Kafka At Least Once 전략으로 인한 중복 메시지 처리 방지
 *
 * 역할:
 * - 처리 완료된 이벤트 ID 기록
 * - 같은 이벤트 재처리 방지
 * - EventLog와 분리된 운영용 테이블
 */
@Entity
@Table(name = "event_handled")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled {

    /**
     * 이벤트 고유 식별자 (Primary Key)
     * 형태: "catalog-events-0-123" (topic-partition-offset)
     * Kafka 메시지의 고유성을 보장하는 복합 키
     */
    @Id
    private String eventId;

    /**
     * 이벤트 타입
     * 예: "LikeAddedEvent", "LikeRemovedEvent", "OrderCreatedEvent"
     */
    private String eventType;

    /**
     * 집계 키 (비즈니스 식별자)
     * 예: productId(123), orderId(456) 등
     * 디버깅 및 추적용
     */
    private String aggregateKey;

    /**
     * 처리 상태
     * SUCCESS: 정상 처리 완료
     * FAILED: 처리 실패 (향후 재시도 가능)
     */
    private String status;


    /**
     * 이벤트 처리 완료 시간
     */
    private LocalDateTime handledAt;

    private EventHandled(String eventId, String eventType, String aggregateKey, String status) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateKey = aggregateKey;
        this.status = status;
        this.handledAt = LocalDateTime.now();
    }


    // 성공 처리(정적 팩토리 메서드)
    public static EventHandled createSuccess(String eventId, String eventType, String aggregateKey) {
        return new EventHandled(eventId, eventType, aggregateKey, "SUCCESS");
    }

    // 실패 처리(정적 팩토리 메서드)
    public static EventHandled createFailed(String eventId, String eventType, String aggregateKey) {
        return new EventHandled(eventId, eventType, aggregateKey, "FAILED");
    }

    // 상태 확인
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

}
