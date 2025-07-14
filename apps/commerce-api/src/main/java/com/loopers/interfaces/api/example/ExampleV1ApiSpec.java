package com.loopers.interfaces.api.example;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/*
    interface : 실제 구현 로직이 없는, 순수한 설계도
    @Tag : Swagger 문서화에서 API 그룹화 및 설명을 위한 어노테이션
    @Operation : Swagger 문서화에서 API의 동작(메소드)에 대한 설명을 추가하는 어노테이션
*/
@Tag(name = "Example V1 API", description = "Loopers 예시 API 입니다.")
public interface ExampleV1ApiSpec {

    @Operation(
        summary = "예시 조회",
        description = "ID로 예시를 조회합니다."
    )
    ApiResponse<ExampleV1Dto.ExampleResponse> getExample(
        @Schema(name = "예시 ID", description = "조회할 예시의 ID")
        Long exampleId
    );
}
