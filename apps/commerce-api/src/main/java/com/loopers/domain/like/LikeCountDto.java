package com.loopers.domain.like;

public record LikeCountDto(
    Long targetId,
    Long count
) {
}
