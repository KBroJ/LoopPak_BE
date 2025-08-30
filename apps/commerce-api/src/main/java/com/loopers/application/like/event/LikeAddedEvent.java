package com.loopers.application.like.event;

import com.loopers.domain.like.LikeType;

public record LikeAddedEvent(
    Long userId,
    Long targetId,
    LikeType likeType
) {
    public static LikeAddedEvent of(Long userId, Long targetId, LikeType likeType) {
        return new LikeAddedEvent(userId, targetId, likeType);
    }
}