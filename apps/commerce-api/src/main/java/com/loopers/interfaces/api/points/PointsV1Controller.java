package com.loopers.interfaces.api.points;

import com.loopers.application.points.PointFacade;
import com.loopers.application.points.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointsV1Controller implements PointsV1ApiSpec{

    private final PointFacade pointFacade;

    @Override
    @GetMapping
    public ApiResponse<PointsV1Dto.PointResponse> getPointInfo(
            @RequestHeader("X-USER-ID")  String userId
    ) {

        PointInfo pointInfo = pointFacade.getPointInfo(userId);

        /*
        return ApiResponse.success(
                // Mocked response for demonstration
                new PointsV1Dto.PointResponse(100L)
        );
        */

        return ApiResponse.success(
                PointsV1Dto.PointResponse.from(pointInfo)
        );
    }

    @Override
    @PostMapping("/charge")
    public ApiResponse<PointsV1Dto.PointResponse> chargePoint(
            @RequestHeader("X-USER-ID")  String userId,
            @RequestBody PointsV1Dto.PointRequest request
    ) {

        PointInfo totalPoint = pointFacade.chargePoint(userId, request.reqPoint());


        // Mocked response for demonstration
        /*
        return ApiResponse.success(
                new PointsV1Dto.PointResponse(1000L)
        );
        */

        return ApiResponse.success(
                PointsV1Dto.PointResponse.from(totalPoint)
        );

    }


}
