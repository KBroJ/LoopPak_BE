package com.loopers.application.like;

import com.loopers.application.like.event.LikeAddedEvent;
import com.loopers.application.like.event.LikeRemovedEvent;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.ProductCacheRepository;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeProcessingService {

    private final ProductService productService;
    private final ProductCacheRepository productCacheRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processLikeAdded(LikeAddedEvent event) {

        log.info("좋아요 추가 이벤트 처리 시작 - userId: {}, targetId: {}, likeType: {}", event.userId(), event.targetId(), event.likeType());

        if (event.likeType() == LikeType.PRODUCT) {
            processProductLikeIncrease(event.targetId());
        }

        log.info("좋아요 추가 집계 처리 완료 - targetId: {}", event.targetId());

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processLikeRemoved(LikeRemovedEvent event) {

        log.info("좋아요 제거 이벤트 처리 시작 - userId: {}, targetId: {}, likeType: {}", event.userId(), event.targetId(), event.likeType());

        if (event.likeType() == LikeType.PRODUCT) {
            processProductLikeDecrease(event.targetId());
        }

        log.info("좋아요 제거 집계 처리 완료 - targetId: {}", event.targetId());

    }

    // 기존 private 메서드들도 그대로 복사
    @Retryable(
        value = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 50,
        backoff = @Backoff(delay = 10, multiplier = 1.5, maxDelay = 1000)
    )
    private void processProductLikeIncrease(Long productId) {

        try {
            productService.increaseLikeCount(productId);

            // Redis 캐시 삭제(상품의 총 좋아요 수)
            productCacheRepository.evictProductDetail(productId);

            log.info("상품 좋아요 수 증가 완료 - productId: {}", productId);
        } catch (Exception e) {
            log.error("상품 좋아요 수 증가 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }

    }

    @Retryable(
        value = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 50,
        backoff = @Backoff(delay = 10, multiplier = 1.5, maxDelay = 1000)
    )
    private void processProductLikeDecrease(Long productId) {

        try {
            productService.decreaseLikeCount(productId);

            // Redis 캐시 삭제(상품의 총 좋아요 수)
            productCacheRepository.evictProductDetail(productId);

            log.info("상품 좋아요 수 감소 완료 - productId: {}", productId);
        } catch (Exception e) {
            log.error("상품 좋아요 수 감소 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }

    }

}
