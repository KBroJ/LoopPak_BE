package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long userId;
    @NotNull
    private Long targetId;
    @Enumerated(EnumType.STRING)
    private LikeType type;

    private Like(Long userId, Long targetId, LikeType type) {

        if (userId == null || userId < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId 는 null이거나, 0 미만일 수 없습니다.");
        }
        if (targetId == null || targetId < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "targetId 는 null이거나, 0 미만일 수 없습니다.");
        }
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "LikeType은 null일 수 없습니다.");
        }

        this.userId = userId;
        this.targetId = targetId;
        this.type = type;
    }

    public static Like of(Long userId, Long targetId, LikeType type) {
        return new Like(userId, targetId, type);
    }
}
