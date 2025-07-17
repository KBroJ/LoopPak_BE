package com.loopers.application.points;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.points.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {

    private final PointService pointService;

    public PointInfo getPointInfo(String userId) {

        PointModel pointModel = pointService.getPointInfo(userId);

        return PointInfo.from(pointModel);

    }

    public PointInfo chargePoint(String userId, Long point) {

        PointModel totalPoint = pointService.chargePoint(userId, point);

        return PointInfo.from(totalPoint);
    }

}
