package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductCacheRepository;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final ProductCacheRepository productCacheRepository;

    /**
     * 상품에 '좋아요'를 등록합니다.
     */
    @Transactional
    @Retryable(
            value = { ObjectOptimisticLockingFailureException.class },  // 이 예외가 발생하면 재시도
            maxAttempts = 20,                                           // 최대 3번 시도
            backoff = @Backoff(delay = 100)                             // 재시도 사이에 100ms 대기
    )
    public void like(Long userId, Long productId, LikeType likeType) {
        try {

            Optional<Like> exist = likeRepository.findByUserIdAndTargetIdAndType(userId, productId, likeType);

            if (exist.isEmpty()) {
                likeRepository.save(Like.of(userId, productId, LikeType.PRODUCT));

                Product product = productRepository.productInfo(productId)
                        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보를 찾을 수 없습니다."));
                product.increaseLikeCount();

                // Redis 캐시 삭제(상품의 총 좋아요 수)
                productCacheRepository.evictProductDetail(productId);
            }

        } catch (DataIntegrityViolationException e) {
            System.out.println("중복 '좋아요' 요청으로 인한 충돌 발생 (정상 처리)");
        }
    }

    /**
     * 상품 '좋아요'를 취소합니다.
     */
    @Transactional
    public void unlike(Long userId, Long productId, LikeType likeType) {

        Optional<Like> likeOptional = likeRepository.findByUserIdAndTargetIdAndType(userId, productId, likeType);

        if (likeOptional.isPresent()) {
            likeRepository.delete(likeOptional.get());

            Product product = productRepository.productInfo(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보를 찾을 수 없습니다."));
            product.decreaseLikeCount();

            // Redis 캐시 삭제(상품의 총 좋아요 수)
            productCacheRepository.evictProductDetail(productId);
        }

    }

}
