package com.loopers.collector.application.eventhandled;

import com.loopers.collector.domain.eventhandled.EventHandled;
import com.loopers.collector.domain.eventhandled.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트 멱등성 처리를 위한 Application Service
 *
 * 역할:
 * - Kafka Consumer에서 중복 메시지 처리 방지
 * - 이벤트 처리 완료 상태 관리
 * - At Least Once → At Most Once 효과 구현
 *
 * 사용 패턴:
 * 1. isAlreadyHandled() → true면 Skip
 * 2. 비즈니스 로직 실행
 * 3. markAsHandled() 완료 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventHandledService {

    private final EventHandledRepository eventHandledRepository;

    /**
     * 이벤트 중복 처리 여부 확인
     * @param eventId "catalog-events-0-123" 형태
     * @return true: 중복(Skip), false: 신규(실행)
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyHandled(String eventId) {
        boolean exists = eventHandledRepository.existsByEventId(eventId);

        if (exists) {
            log.info("중복 이벤트 감지 - eventId: {}", eventId);
        } else {
            log.debug("신규 이벤트 - eventId: {}", eventId);
        }

        return exists;
    }

    /**
     * 이벤트 처리 완료 기록
     */
    @Transactional
    public void markAsHandled(String eventId, String eventType, String aggregateKey) {
        try {
            EventHandled handled = EventHandled.createSuccess(eventId, eventType, aggregateKey);
            eventHandledRepository.save(handled);

            log.info("이벤트 처리 완료 기록 - eventId: {}, eventType: {}", eventId, eventType);
        } catch (Exception e) {
            log.error("처리 완료 기록 실패 - eventId: {}, error: {}", eventId, e.getMessage(), e);
            throw e;
        }
    }

}
