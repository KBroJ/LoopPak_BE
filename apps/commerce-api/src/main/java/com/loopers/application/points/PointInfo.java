package com.loopers.application.points;

import com.loopers.domain.points.Point;

public record PointInfo (
    String userId, Long point
) {

    public static PointInfo from(Point point) {
        return new PointInfo(
            point.getUser().getUserId(),
            point.getPoint()
        );
    }

}
