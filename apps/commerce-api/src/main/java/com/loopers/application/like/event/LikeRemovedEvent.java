package com.loopers.application.like.event;

import com.loopers.domain.like.LikeType;

public record LikeRemovedEvent(
    Long userId,
    Long targetId,
    LikeType likeType
) {
    public static LikeRemovedEvent of(Long userId, Long targetId, LikeType likeType) {
        return new LikeRemovedEvent(userId, targetId, likeType);
    }
}