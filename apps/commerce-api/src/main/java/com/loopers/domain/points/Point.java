package com.loopers.domain.points;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.users.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;
    @NotNull
    private Long point;

    public Point(User user, Long point) {
        this.user = user;
        validatePoint(point);
        this.point = point;
    }

    public static Point of(User user, Long point) {
        return new Point(user, point);
    }

    private void validatePoint(Long point) {
        if (point == null || point < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 포인트는 0 초과이어야 합니다.");
        }
    }

    public void charge(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전할 포인트는 0보다 커야 합니다.");
        }
        this.point += amount;
    }

    public void use(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 포인트는 0보다 커야 합니다.");
        }
        if (this.point < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }
        this.point -= amount;
    }

}
