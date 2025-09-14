package com.loopers.application.eventlog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.eventlog.EventLog;
import com.loopers.domain.eventlog.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트 감사 로그 처리 서비스
 * 모든 도메인 이벤트를 영구 저장하여 추적성과 디버깅을 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * 이벤트를 감사 로그로 저장
     *
     * @param event 저장할 이벤트 객체
     * @param eventType 이벤트 타입 (클래스명)
     * @param aggregateId 집계 루트 ID (productId, orderId 등)
     * @param aggregateType 집계 루트 타입 (PRODUCT, ORDER 등)
     */
    @Transactional
    public void saveEventLog(Object event, String eventType, String aggregateId, String aggregateType) {
        try {
            // 멱등성을 위한 고유 이벤트 ID 생성
            String eventId = generateEventId(eventType, aggregateId, event);

            // 중복 이벤트 확인 (멱등성 보장)
            if (eventLogRepository.existsByEventId(eventId)) {
                log.debug("이미 처리된 이벤트 - eventId: {}", eventId);
                return;
            }

            // 이벤트 객체를 JSON으로 직렬화
            String eventData = objectMapper.writeValueAsString(event);

            // EventLog 엔티티 생성 및 저장
            EventLog eventLog = EventLog.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventData(eventData)
                    .build();

            eventLogRepository.save(eventLog);

            log.info("이벤트 감사 로그 저장 완료 - eventId: {}, eventType: {}", eventId, eventType);

        } catch (Exception e) {
            log.error("이벤트 감사 로그 저장 실패 - eventType: {}, aggregateId: {}, error: {}",
                    eventType, aggregateId, e.getMessage(), e);
            throw new RuntimeException("감사 로그 저장 실패", e);
        }
    }

    /**
     * 멱등성을 위한 고유 이벤트 ID 생성
     * 형식: "EventType_AggregateId_Hash"
     */
    private String generateEventId(String eventType, String aggregateId, Object event) {
        try {
            // 이벤트 내용의 해시값으로 고유성 보장
            String eventData = objectMapper.writeValueAsString(event);
            int hashCode = eventData.hashCode();

            return String.format("%s_%s_%s_%d",
                    eventType,
                    aggregateId,
                    System.currentTimeMillis() / 1000, // 초 단위 타임스탬프
                    Math.abs(hashCode)
            );
        } catch (Exception e) {
            // 해시 생성 실패 시 타임스탬프 기반 ID
            return String.format("%s_%s_%d",
                    eventType,
                    aggregateId,
                    System.currentTimeMillis()
            );
        }
    }

}
