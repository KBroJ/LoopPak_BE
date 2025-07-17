package com.loopers.interfaces.api.points;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.users.UsersV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointsV1Controller implements PointsV1ApiSpec{

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
    public ApiResponse<UsersV1Dto.UsersResponse> chargePoint(
            @RequestHeader("X-USER-ID")  String userId,
            @RequestBody PointsV1Dto.PointRequest request
    ) {
        return null;
    }


}
