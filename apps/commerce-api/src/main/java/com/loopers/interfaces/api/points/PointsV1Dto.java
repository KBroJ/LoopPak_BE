package com.loopers.interfaces.api.points;

public class PointsV1Dto {

    public record PointRequest(
        Long reqPoint
    ) {
    }

    public record PointResponse(
        Long totalPoint
    ) {

    }

}
