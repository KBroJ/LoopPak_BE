package com.loopers.application.points;

import com.loopers.domain.points.PointModel;

public record PointInfo (
    String userId, Long point
) {

    public static PointInfo from(PointModel pointModel) {
        return new PointInfo(
            pointModel.getUserModel().getUserId(),
            pointModel.getPoint()
        );
    }

}
