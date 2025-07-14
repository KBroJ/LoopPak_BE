package com.loopers.interfaces.api.users;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.example.ExampleV1Dto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Users V1 API", description = "회원가입, 내 정보 조회 API 입니다.")
public interface UsersV1ApiSpec {

    @Operation(
            summary = "회원가입",
            description = "ID로 예시를 조회합니다."
    )
    ApiResponse<UsersV1Dto.UsersResponse> saveUser(
            @Schema(name = "예시 ID", description = "조회할 예시의 ID")
            UsersV1Dto.UsersSaveRequest request
    );

    @Operation(
            summary = "내 정보 조회",
            description = "ID로 회원 정보를 조회합니다."
    )
    ApiResponse<UsersV1Dto.UsersResponse> getUserInfo(
            @Schema(name = "예시 ID", description = "조회할 예시의 ID")
            Long exampleId
    );

}
