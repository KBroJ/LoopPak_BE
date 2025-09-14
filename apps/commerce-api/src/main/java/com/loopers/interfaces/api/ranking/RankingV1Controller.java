package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;
import com.loopers.application.ranking.RankingQueryService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 랭킹 API 컨트롤러
 *
 * 역할:
 * - HTTP 요청 파라미터 검증 및 변환
 * - RankingQueryService 호출
 * - 응답 데이터 변환 및 반환
 *
 * API 엔드포인트:
 * - GET /api/v1/rankings - 상품 랭킹 조회
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingQueryService rankingQueryService;

    // 날짜 형식 상수
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 상품 랭킹 조회 API
     *
     * 처리 흐름:
     * 1. 요청 파라미터 검증 및 기본값 설정
     * 2. RankingQueryService 호출
     * 3. 응답 데이터 반환
     *
     * 파라미터 처리:
     * - date: null이면 오늘 날짜 자동 설정
     * - size: 기본값 20, 최대 100으로 제한
     * - page: 기본값 0, 음수면 0으로 보정
     */
    @GetMapping("/api/v1/rankings")
    @Override
    public ApiResponse<RankingV1Dto.PageResponse> getRankings(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {

        try {
            // 1. 요청 파라미터 검증 및 정규화
            String normalizedDate = normalizeDate(date);
            int normalizedSize = normalizeSize(size);
            int normalizedPage = normalizePage(page);

            log.info("랭킹 조회 요청 - date: {}, size: {}, page: {}",
                    normalizedDate, normalizedSize, normalizedPage);

            // 2. RankingQueryService 호출
            RankingInfo.PageResult serviceResult = rankingQueryService.getRankings(
                    normalizedDate, normalizedSize, normalizedPage
            );

            RankingV1Dto.PageResponse rankingResponse = convertToDto(serviceResult);

            log.info("랭킹 조회 완료 - totalProducts: {}, returnedItems: {}",
                    rankingResponse.meta().totalProducts(),
                    rankingResponse.rankings().size());

            // 3. 성공 응답 반환
            return ApiResponse.success(rankingResponse);

        } catch (Exception e) {
            log.error("랭킹 조회 실패 - date: {}, size: {}, page: {}, error: {}",
                    date, size, page, e.getMessage(), e);
            throw e; // Global Exception Handler가 처리
        }
    }

    /**
     * 날짜 파라미터 정규화
     *
     * @param date 요청된 날짜 (yyyyMMdd 형식 또는 null)
     * @return 정규화된 날짜 (yyyyMMdd 형식)
     */
    private String normalizeDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            // 기본값: 오늘 날짜
            return LocalDate.now().format(DATE_FORMAT);
        }

        // TODO: 날짜 형식 검증 로직 추가 (yyyyMMdd 패턴 체크)
        // 현재는 단순히 trim만 수행
        return date.trim();
    }

    /**
     * 페이지 크기 정규화
     *
     * @param size 요청된 페이지 크기
     * @return 정규화된 페이지 크기 (1~100 범위)
     */
    private int normalizeSize(int size) {
        if (size <= 0) {
            return 20; // 기본값
        }
        if (size > 100) {
            return 100; // 최대값 제한
        }
        return size;
    }

    /**
     * 페이지 번호 정규화
     *
     * @param page 요청된 페이지 번호
     * @return 정규화된 페이지 번호 (0 이상)
     */
    private int normalizePage(int page) {
        return Math.max(0, page); // 음수면 0으로 보정
    }

    /**
     * RankingInfo → RankingV1Dto 변환
     *
     * Application Layer DTO를 Interface Layer DTO로 변환
     * - 레이어 의존성 준수: Application → Interface 단방향 변환
     * - Controller의 책임: HTTP 응답 형태로 데이터 변환
     *
     * @param pageResult Application Layer의 랭킹 결과
     * @return Interface Layer의 HTTP 응답 DTO
     */
    private RankingV1Dto.PageResponse convertToDto(RankingInfo.PageResult pageResult) {

        // RankingItem 변환
        List<RankingV1Dto.RankingItem> dtoRankings = pageResult.rankings().stream()
                .map(this::convertRankingItem)
                .collect(Collectors.toList());

        // PaginationInfo 변환
        RankingV1Dto.PaginationInfo dtoPagination = RankingV1Dto.PaginationInfo.of(
                pageResult.pagination().currentPage(),
                pageResult.pagination().size(),
                pageResult.pagination().totalElements()
        );

        // RankingMeta 변환
        RankingV1Dto.RankingMeta dtoMeta = RankingV1Dto.RankingMeta.of(
                pageResult.meta().date(),
                pageResult.meta().totalProducts()
        );

        return RankingV1Dto.PageResponse.of(dtoRankings, dtoPagination, dtoMeta);
    }

    /**
     * RankingInfo.RankingItem → RankingV1Dto.RankingItem 변환
     *
     * @param item Application Layer의 랭킹 아이템
     * @return Interface Layer의 랭킹 아이템 DTO
     */
    private RankingV1Dto.RankingItem convertRankingItem(RankingInfo.RankingItem item) {
        RankingV1Dto.ProductInfo dtoProduct = RankingV1Dto.ProductInfo.of(
                item.product().id(),
                item.product().name(),
                item.product().price(),
                item.product().brandId(),
                item.product().brandName(),
                item.product().likeCount()
        );

        return RankingV1Dto.RankingItem.of(item.rank(), item.score(), dtoProduct);
    }

}
