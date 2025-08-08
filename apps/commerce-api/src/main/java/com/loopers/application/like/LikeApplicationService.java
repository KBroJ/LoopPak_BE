package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeCountDto;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeApplicationService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    /**
     * 상품에 '좋아요'를 등록합니다.
     */
    @Transactional
    public void like(Long userId, Long productId, LikeType likeType) {
        try {

            Optional<Like> exist = likeRepository.findByUserIdAndTargetIdAndType(userId, productId, likeType);

            if (exist.isEmpty()) {
                likeRepository.save(Like.of(userId, productId, LikeType.PRODUCT));
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
        try {
//            likeRepository.findByUserIdAndTargetIdAndType(userId, productId, likeType)
//                    .ifPresent(likeRepository::delete);
            likeRepository.deleteByUserIdAndTargetIdAndType(userId, productId, likeType);
        } catch (ObjectOptimisticLockingFailureException e) {
            System.out.println("'좋아요 취소' 중 낙관적 락 충돌 발생 (정상 처리)");
        }
    }

    /**
     * 사용자가 좋아요 한 상품 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<Product> getLikedProducts(Long userId, LikeType likeType, int page, int size) {
        // 1. 사용자가 '좋아요'한 상품 ID 목록을 먼저 조회합니다.
        List<Long> likedProductIds = likeRepository.findByUserIdAndType(userId, likeType)
                .stream()
                .map(Like::getTargetId)
                .toList();

        // 2. 해당 ID 목록을 가지고 Product 페이지를 조회합니다.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return productRepository.findByIdIn(likedProductIds, pageable);
    }

    public Map<Long, Long> getLikeCounts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return likeRepository.countByTargetIdIn(productIds, LikeType.PRODUCT)
                .stream()
                .collect(Collectors.toMap(LikeCountDto::targetId, LikeCountDto::count));
    }

}
