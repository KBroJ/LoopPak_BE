package com.loopers.domain.points;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.users.UserModel;
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
public class PointModel extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private UserModel userModel;
    @NotNull
    private Long point;

    public PointModel(UserModel userModel, Long point) {
        this.userModel = userModel;
        validatePoint(point);
        this.point = point;
    }

    public static PointModel of(UserModel userModel, Long point) {
        return new PointModel(userModel, point);
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

}
