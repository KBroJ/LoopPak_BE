package com.loopers.interfaces.api.points;

import com.loopers.application.points.PointFacade;
import com.loopers.application.users.UserFacade;
import com.loopers.application.users.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointsV1Controller implements PointsV1ApiSpec{

    private final UserFacade userFacade;
//    private final PointFacade pointFacade;

    @Override
    @GetMapping
    public ApiResponse<PointsV1Dto.PointResponse> getPointInfo(
            @RequestHeader("X-USER-ID")  String userId
    ) {
//        return null;
        return ApiResponse.success(
                // Mocked response for demonstration
                new PointsV1Dto.PointResponse(100L)
        );
    }

    @Override
    @PostMapping("/charge")
    public ApiResponse<PointsV1Dto.PointResponse> chargePoint(
            @RequestHeader("X-USER-ID")  String userId,
            @RequestBody PointsV1Dto.PointRequest request
    ) {

        UserInfo userInfo = userFacade.getMyInfo(userId);

//        return null;
        return ApiResponse.success(
                // Mocked response for demonstration
                new PointsV1Dto.PointResponse(1000L)
        );

    }


}
