package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventHandledRepositoryImpl implements EventHandledRepository {

    private final EventHandledJpaRepository eventHandledJpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return eventHandledJpaRepository.existsByEventId(eventId);
    }

    // INSERT 또는 UPDATE 자동 판단
    @Override
    public EventHandled save(EventHandled eventHandled) {
        return eventHandledJpaRepository.save(eventHandled);
    }

}
