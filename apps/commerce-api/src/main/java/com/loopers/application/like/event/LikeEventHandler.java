package com.loopers.application.like.event;

import com.loopers.application.like.LikeProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * 좋아요 관련 이벤트 처리 핸들러
 * 집계 처리와 캐시 관리를 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {

    private final LikeProcessingService likeProcessingService;

    @EventListener
    @Async
    public void handleLikeAdded(LikeAddedEvent event) {
        log.info("좋아요 추가 이벤트 처리 시작 - userId: {}, targetId: {}, likeType: {}", event.userId(), event.targetId(), event.likeType());

        likeProcessingService.processLikeAdded(event);

        log.info("좋아요 추가 집계 처리 완료 - targetId: {}", event.targetId());
    }

    @EventListener
    @Async
    public void handleLikeRemoved(LikeRemovedEvent event) {
        log.info("좋아요 제거 이벤트 처리 시작 - userId: {}, targetId: {}, likeType: {}", event.userId(), event.targetId(), event.likeType());

        likeProcessingService.processLikeRemoved(event);

        log.info("좋아요 제거 집계 처리 완료 - targetId: {}", event.targetId());
    }

}