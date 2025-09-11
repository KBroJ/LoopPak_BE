package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 랭킹 API 명세
 *
 * 제공 기능:
 * - 일별 상품 랭킹 조회 (Top-N)
 * - 페이징 지원
 * - 상품 정보 포함 (Aggregation)
 */
@Tag(name = "랭킹 API", description = "상품 랭킹 조회 API")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "상품 랭킹 조회",
            description = """
              일별 상품 랭킹을 조회합니다.

              🔍 조회 조건:
              - date: 조회할 날짜 (yyyyMMdd 형식, 기본값: 오늘)
              - size: 한 페이지당 상품 수 (기본값: 20)
              - page: 페이지 번호 (0부터 시작, 기본값: 0)

              📊 랭킹 기준:
              - 좋아요: +0.2점
              - 주문: +0.7점 (향후 확장)
              - 조회: +0.1점 (향후 확장)

              ⚡ 성능:
              - Redis ZSET 기반 고속 조회
              - 상품 정보 포함 (이름, 가격, 브랜드 등)
              """
    )
    ApiResponse<RankingV1Dto.PageResponse> getRankings(
            @Parameter(
                    description = "조회할 날짜 (yyyyMMdd 형식)",
                    example = "20250111"
            ) String date,

            @Parameter(
                    description = "페이지당 상품 수",
                    example = "20"
            ) int size,

            @Parameter(
                    description = "페이지 번호 (0부터 시작)",
                    example = "0"
            ) int page
    );

}
