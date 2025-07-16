package com.loopers.interfaces.api.points;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.users.UsersV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Points V1 API", description = "포인트 조회, 포인트 충전 API 입니다.")
public interface PointsV1ApiSpec {

    @Operation(
            summary = "포인트 조회",
            description = "헤더의 X-USER-ID로 들어오는 ID로 포인트를 조회합니다."
    )
    ApiResponse<PointsV1Dto.PointResponse> getPointInfo(
            @Schema(name = "예시 ID", description = "조회할 예시의 ID")
            @RequestHeader("X-USER-ID") String userId
    );

    @Operation(
            summary = "포인트 충전",
            description = "ID로 포인트를 충전하고 포인트 보유 총량을 반환합니다."
    )
    ApiResponse<PointsV1Dto.PointResponse> chargePoint(
            @Schema(name = "예시 ID", description = "조회할 예시의 ID")
            @RequestHeader("X-USER-ID") String userId,
            @Schema(name = "예시 ID", description = "조회할 예시의 ID")
            PointsV1Dto.PointRequest request
    );

}
