package com.loopers.application.like;

import com.loopers.application.like.event.LikeAddedEvent;
import com.loopers.application.like.event.LikeRemovedEvent;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeRepository likeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 상품에 '좋아요'를 등록합니다.
     * 좋아요 처리(핵심)와 집계 처리(후속)를 이벤트로 분리
     */
    @Transactional
    public void like(Long userId, Long productId, LikeType likeType) {
        try {
            Optional<Like> exist = likeRepository.findByUserIdAndTargetIdAndType(userId, productId, likeType);

            if (exist.isEmpty()) {
                // 1. 핵심 트랜잭션: Like 엔티티 저장
                likeRepository.save(Like.of(userId, productId, likeType));
                log.info("좋아요 등록 완료 - userId: {}, targetId: {}, likeType: {}", userId, productId, likeType);

                // 2. 후속 처리: 집계 이벤트 발행 (커밋 후 처리)
                LikeAddedEvent event = LikeAddedEvent.of(userId, productId, likeType);
                eventPublisher.publishEvent(event);
                log.info("좋아요 추가 이벤트 발행 - userId: {}, targetId: {}", userId, productId);
            } else {
                log.debug("이미 좋아요가 존재함 - userId: {}, targetId: {}", userId, productId);
            }

        } catch (DataIntegrityViolationException e) {
            log.warn("중복 좋아요 요청으로 인한 충돌 발생 (정상 처리) - userId: {}, targetId: {}", userId, productId);
        }
    }

    /**
     * 상품 '좋아요'를 취소합니다.
     * 좋아요 취소(핵심)와 집계 처리(후속)를 이벤트로 분리
     */
    @Transactional
    public void unlike(Long userId, Long productId, LikeType likeType) {
        Optional<Like> likeOptional = likeRepository.findByUserIdAndTargetIdAndType(userId, productId, likeType);

        if (likeOptional.isPresent()) {
            // 1. 핵심 트랜잭션: Like 엔티티 삭제
            likeRepository.delete(likeOptional.get());
            log.info("좋아요 취소 완료 - userId: {}, targetId: {}, likeType: {}", userId, productId, likeType);

            // 2. 후속 처리: 집계 이벤트 발행 (커밋 후 처리)
            LikeRemovedEvent event = LikeRemovedEvent.of(userId, productId, likeType);
            eventPublisher.publishEvent(event);
            log.info("좋아요 제거 이벤트 발행 - userId: {}, targetId: {}", userId, productId);
        } else {
            log.debug("취소할 좋아요가 존재하지 않음 - userId: {}, targetId: {}", userId, productId);
        }
    }

}
