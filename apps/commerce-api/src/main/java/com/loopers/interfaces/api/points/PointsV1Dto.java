package com.loopers.interfaces.api.points;

import com.loopers.application.points.PointInfo;

public class PointsV1Dto {

    public record PointRequest(
        Long reqPoint
    ) {
    }

    public record PointResponse(
        Long totalPoint
    ) {
        public static PointsV1Dto.PointResponse from(PointInfo pointInfo) {
            return new PointsV1Dto.PointResponse(
                pointInfo.point()
            );
        }
    }

}
