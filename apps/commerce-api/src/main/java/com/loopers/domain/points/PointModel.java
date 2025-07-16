package com.loopers.domain.points;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.users.UserModel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointModel extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private UserModel userModel;

    private Long point;


    public UserModel getUserModel() {
        return userModel;
    }

    public Long getPoint() {
        return point;
    }

    public PointModel(UserModel userModel, Long point) {
        this.userModel = userModel;
        this.point = point;
    }

    public static PointModel of(UserModel userModel, Long point) {
        return new PointModel(userModel, point);
    }




}
