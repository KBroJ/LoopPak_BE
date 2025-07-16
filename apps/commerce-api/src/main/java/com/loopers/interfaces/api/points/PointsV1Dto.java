package com.loopers.interfaces.api.points;

public class PointsV1Dto {

    public record PointRequest(
        String userId,
        Long point
    ) {
    }

    public record PointResponse(
        Long totalPoint
    ) {

    }

}
