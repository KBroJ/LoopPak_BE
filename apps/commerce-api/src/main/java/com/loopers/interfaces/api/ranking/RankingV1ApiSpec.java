package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 랭킹 API 명세
 *
 * 제공 기능:
 * - 일간/주간/월간 상품 랭킹 조회 (Top-N)
 * - 페이징 지원
 * - 상품 정보 포함 (Aggregation)
 * - period 파라미터를 통한 기간별 랭킹 제공
 */
@Tag(name = "랭킹 API", description = "상품 랭킹 조회 API")
public interface RankingV1ApiSpec {

    @Operation(
            summary = "상품 랭킹 조회",
            description = """
              일별 상품 랭킹을 조회합니다.

                🔍 조회 조건:
               - period: 조회 기간 (daily, weekly, monthly, 기본값: daily)
               - date: 조회할 날짜 (yyyyMMdd 형식, 기본값: 오늘)
               - size: 한 페이지당 상품 수 (기본값: 20)
               - page: 페이지 번호 (0부터 시작, 기본값: 0)

               📊 기간별 데이터 소스:
               - daily: Redis ZSET 기반 실시간 랭킹 (좋아요, 조회수, 판매량)
               - weekly: DB 배치 기반 주간 랭킹 (매주 집계된 TOP 100)
               - monthly: DB 배치 기반 월간 랭킹 (매월 집계된 TOP 100)

               📅 날짜 기준:
               - daily: 해당 날짜의 실시간 랭킹
               - weekly: 해당 날짜가 포함된 주의 랭킹
               - monthly: 해당 날짜가 포함된 월의 랭킹

              ⚡ 성능:
                - Redis Zset 기반 고속 조회 (일간)
                - DB 기반 안정적 조회 (주간/월간)
                - 상품 정보 포함 (이름, 가격, 브랜드 등)
              """
    )
    ApiResponse<RankingV1Dto.PageResponse> getRankings(
            @Parameter(
                    description = """
                              조회 기간 타입
                              - daily: 일간 랭킹 (기본값, Redis 실시간)
                              - weekly: 주간 랭킹 (DB 배치)
                              - monthly: 월간 랭킹 (DB 배치)
                              """,
                    example = "daily"
            ) String period,
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
