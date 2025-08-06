package com.loopers.domain.like;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndTargetIdAndType(Long userId, Long targetId, LikeType likeType);

    void save(Like like);

    void delete(Like like);

    List<Like> findByUserIdAndType(Long userId, LikeType likeType);

    List<LikeCountDto> countByTargetIdIn(List<Long> targetIds, LikeType type);

    Page<Long> findProductIdsOrderByLikesDesc(Long brandId, Pageable idPageable);

    long getLikeCount(Long targetId);
}
