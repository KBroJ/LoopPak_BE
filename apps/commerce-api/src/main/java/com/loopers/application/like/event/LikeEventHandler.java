package com.loopers.application.like.event;

import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCacheRepository;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 관련 이벤트 처리 핸들러
 * 집계 처리와 캐시 관리를 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {

    private final ProductRepository productRepository;
    private final ProductCacheRepository productCacheRepository;

    @Transactional
    @EventListener
    public void handleLikeAdded(LikeAddedEvent event) {
        log.info("좋아요 추가 이벤트 처리 시작 - userId: {}, targetId: {}, likeType: {}", 
                event.userId(), event.targetId(), event.likeType());

        if (event.likeType() == LikeType.PRODUCT) {
            processProductLikeIncrease(event.targetId());
        }

        log.info("좋아요 추가 집계 처리 완료 - targetId: {}", event.targetId());
    }

    @Transactional
    @EventListener
    public void handleLikeRemoved(LikeRemovedEvent event) {
        log.info("좋아요 제거 이벤트 처리 시작 - userId: {}, targetId: {}, likeType: {}", 
                event.userId(), event.targetId(), event.likeType());

        if (event.likeType() == LikeType.PRODUCT) {
            processProductLikeDecrease(event.targetId());
        }

        log.info("좋아요 제거 집계 처리 완료 - targetId: {}", event.targetId());
    }

    @Retryable(
            value = { ObjectOptimisticLockingFailureException.class },
            maxAttempts = 50,
            backoff = @Backoff(delay = 10, multiplier = 1.5, maxDelay = 1000)
    )
    private void processProductLikeIncrease(Long productId) {
        try {
            Product product = productRepository.productInfo(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
            
            product.increaseLikeCount();
            productRepository.save(product);
            
            // Redis 캐시 삭제(상품의 총 좋아요 수)
            productCacheRepository.evictProductDetail(productId);
            
            log.info("상품 좋아요 수 증가 완료 - productId: {}, 현재 좋아요 수: {}", productId, product.getLikeCount());
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
            Product product = productRepository.productInfo(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
            
            product.decreaseLikeCount();
            productRepository.save(product);
            
            // Redis 캐시 삭제(상품의 총 좋아요 수)
            productCacheRepository.evictProductDetail(productId);
            
            log.info("상품 좋아요 수 감소 완료 - productId: {}, 현재 좋아요 수: {}", productId, product.getLikeCount());
        } catch (Exception e) {
            log.error("상품 좋아요 수 감소 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }
}