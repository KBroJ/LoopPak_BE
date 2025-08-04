package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndTargetIdAndType(Long userId, Long targetId, LikeType likeType);

    void save(Like like);

    void delete(Like like);

    List<Like> findByUserIdAndType(Long userId, LikeType likeType);

    List<LikeCountDto> countByTargetIdIn(List<Long> targetIds, LikeType type);
}
