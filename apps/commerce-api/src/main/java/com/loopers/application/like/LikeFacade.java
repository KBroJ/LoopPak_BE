package com.loopers.application.like;

import com.loopers.application.like.event.LikeAddedEvent;
import com.loopers.application.like.event.LikeRemovedEvent;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import com.loopers.infrastructure.kafka.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeRepository likeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaEventPublisher kafkaEventPublisher;

    /**
     * 상품에 '좋아요'를 등록합니다.
     * 좋아요 처리(핵심)와 집계 처리(후속)를 이벤트로 분리
     */
    @Transactional
    public void like(Long userId, Long productId, LikeType likeType) {
        try {
            // UPSERT 실행 - 원자적 연산으로 Race Condition 방지
            int affected = likeRepository.upsertLike(userId, productId, likeType);
            
            if (affected == 1) { // INSERT된 경우에만 (UPDATE 제외)
                log.info("좋아요 UPSERT 완료 - userId: {}, targetId: {}, likeType: {}", userId, productId, likeType);
                
                // 이벤트 발행 - 삽입/업데이트된 경우에만
                LikeAddedEvent event = LikeAddedEvent.of(userId, productId, likeType);
                
                // 1. 동기 집계 처리 (ApplicationEvent)
                eventPublisher.publishEvent(event);
                
                // 2. 비동기 외부 시스템 연동 (Kafka)
                kafkaEventPublisher.publish("catalog-events", productId.toString(), event);

                log.info("좋아요 추가 이벤트 발행 - userId: {}, targetId: {}", userId, productId);
            }
            
        } catch (Exception e) {
            log.error("좋아요 UPSERT 처리 중 예외 발생 - userId: {}, targetId: {}, error: {}", userId, productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 상품 '좋아요'를 취소합니다.
     * 좋아요 취소(핵심)와 집계 처리(후속)를 이벤트로 분리
     */
    @Transactional(timeout = 2)
    @Retryable(
            value = { CannotAcquireLockException.class, DeadlockLoserDataAccessException.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, multiplier = 1.5, maxDelay = 500, random = true)
    )
    public void unlike(Long userId, Long productId, LikeType likeType) {
        try {
            // 비관적 락으로 좋아요 조회
            Optional<Like> likeOptional = likeRepository.findByUserIdAndTargetIdAndTypeWithLock(userId, productId, likeType);

            if (likeOptional.isPresent()) {
                // 1. 핵심 트랜잭션: Like 엔티티 삭제
                likeRepository.delete(likeOptional.get());
                log.info("좋아요 취소 완료 - userId: {}, targetId: {}, likeType: {}", userId, productId, likeType);

                // 2. 후속 처리: 집계 이벤트 발행 (커밋 후 처리)
                LikeRemovedEvent event = LikeRemovedEvent.of(userId, productId, likeType);
                
                // 1. 동기 집계 처리 (ApplicationEvent)
                eventPublisher.publishEvent(event);
                
                // 2. 비동기 외부 시스템 연동 (Kafka)
                kafkaEventPublisher.publish("catalog-events", productId.toString(), event);

                log.info("좋아요 제거 이벤트 발행 - userId: {}, targetId: {}", userId, productId);
            } else {
                log.debug("취소할 좋아요가 존재하지 않음 - userId: {}, targetId: {}", userId, productId);
            }

        } catch (CannotAcquireLockException | DeadlockLoserDataAccessException e) {
            log.warn("좋아요 취소 - 락 충돌/데드락 발생으로 인한 재시도 - userId: {}, targetId: {}, error: {}", userId, productId, e.getClass().getSimpleName());
            throw e; // @Retryable이 재시도 처리
        } catch (Exception e) {
            log.error("좋아요 취소 처리 중 예외 발생 - userId: {}, targetId: {}, error: {}", userId, productId, e.getMessage(), e);
            throw e;
        }
    }

}
