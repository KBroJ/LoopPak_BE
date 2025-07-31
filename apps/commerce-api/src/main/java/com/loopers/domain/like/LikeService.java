package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;

    /**
     * 상품에 대한 '좋아요'를 추가합니다.
     */
    @Transactional
    public void like(Long userId, Long targetId, LikeType likeType) {

        Optional<Like> exsist = likeRepository.findByUserIdAndTargetIdAndType(userId, targetId, likeType);

        if(exsist.isEmpty()) {
            likeRepository.save(Like.of(userId, targetId, likeType));
        }

    }

    /**
     * 상품에 대한 '좋아요'를 해제합니다.
     */
    @Transactional
    public void unLike(Long userId, Long targetId, LikeType likeType) {

        Optional<Like> exsist = likeRepository.findByUserIdAndTargetIdAndType(userId, targetId, likeType);

        if(exsist.isPresent()) {
            likeRepository.delete(exsist.get());
        }

    }

    public List<Long> likeList(Long userId, LikeType likeType) {
        List<Like> userLikes = likeRepository.findByUserIdAndType(userId, likeType);

        return userLikes.stream()
                .map(Like::getTargetId)
                .toList();
    }

    /**
     * 여러 상품 ID에 대한 좋아요 수를 Map 형태로 반환합니다.
     * @param productIds 상품 ID 리스트
     * @return Map<productId, likeCount>
     */
    public Map<Long, Long> getLikeCounts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return likeRepository.countByTargetIdIn(productIds, LikeType.PRODUCT)
                .stream()
                .collect(Collectors.toMap(LikeCountDto::targetId, LikeCountDto::count));
    }
}
