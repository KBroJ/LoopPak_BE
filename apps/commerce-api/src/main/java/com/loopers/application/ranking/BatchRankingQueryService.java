package com.loopers.application.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingRepository;
import com.loopers.domain.ranking.WeeklyProductRanking;
import com.loopers.domain.ranking.MonthlyProductRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 배치 랭킹 조회 서비스 (Application Layer)
 *
 * 역할:
 * - Materialized View 기반 주간/월간 랭킹 조회
 * - Spring Batch로 생성된 데이터 조회 전담
 * - 실시간 랭킹(Redis)과 분리하여 책임 명확화
 *
 * 처리 흐름:
 * 1. DB Materialized View에서 랭킹 데이터 조회
 * 2. 상품 정보 Aggregation (ID → 상품 상세 정보)
 * 3. 페이징 처리 및 응답 데이터 변환
 * 4. RankingInfo 반환 (기존과 동일한 응답 구조)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRankingQueryService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final RankingRepository rankingRepository;

    /**
     * 주간 랭킹 조회
     *
     * 처리 흐름:
     * 1. yyyyMMdd → yearWeek 변환
     * 2. mv_product_rank_weekly 테이블에서 페이징 조회
     * 3. 상품 정보 일괄 조회 및 결합
     * 4. RankingInfo 응답 DTO 생성
     *
     * @param date 조회 날짜 (yyyyMMdd) - 해당 날짜가 포함된 주의 랭킹 조회
     * @param size 페이지 크기
     * @param page 페이지 번호 (0부터 시작)
     * @return 페이징된 주간 랭킹 응답
     */
    public RankingInfo.PageResult getWeeklyRankings(String date, int size, int page) {
        try {
            // 1. yyyyMMdd → yearWeek 변환
            String yearWeek = convertDateToYearWeek(date);
            log.debug("주간 랭킹 조회 시작 - date: {}, yearWeek: {}, size: {}, page: {}",
                    date, yearWeek, size, page);

            // 2. Repository에서 주간 랭킹 조회
            List<RankingItem> rankingItems = rankingRepository.getWeeklyTopRankings(yearWeek, size, page);
            log.debug("주간 랭킹 Repository 조회 완료 - yearWeek: {}, 조회된 아이템 수: {}",
                    yearWeek, rankingItems.size());

            // 3. 전체 랭킹 상품 수 조회 (페이징 메타데이터용)
            long totalProducts = rankingRepository.getTotalWeeklyRankingCount(yearWeek);
            log.debug("주간 랭킹 전체 상품 수: {}", totalProducts);

            // 4. 상품 정보 일괄 조회 및 결합 (기존 로직 재사용)
            List<RankingInfo.RankingItem> dtoItems = aggregateProductInfo(rankingItems);
            log.debug("주간 랭킹 상품 정보 결합 완료 - 최종 아이템 수: {}", dtoItems.size());

            // 5. 페이징 메타데이터 생성
            RankingInfo.PaginationInfo pagination = RankingInfo.PaginationInfo.of(page, size, totalProducts);
            RankingInfo.RankingMeta meta = RankingInfo.RankingMeta.of(yearWeek, totalProducts);

            // 6. 최종 응답 DTO 생성
            RankingInfo.PageResult response = RankingInfo.PageResult.of(dtoItems, pagination, meta);

            log.info("주간 랭킹 조회 완료 - date: {}, yearWeek: {}, totalProducts: {}, returnedItems: {}",
                    date, yearWeek, totalProducts, dtoItems.size());

            return response;

        } catch (Exception e) {
            log.error("주간 랭킹 조회 실패 - date: {}, size: {}, page: {}, error: {}",
                    date, size, page, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 월간 랭킹 조회
     *
     * 주간 랭킹과 동일한 패턴, yearMonth 사용
     */
    public RankingInfo.PageResult getMonthlyRankings(String date, int size, int page) {
        try {
            // 1. yyyyMMdd → yearMonth 변환
            String yearMonth = convertDateToYearMonth(date);
            log.debug("월간 랭킹 조회 시작 - date: {}, yearMonth: {}, size: {}, page: {}",
                    date, yearMonth, size, page);

            // 2. Repository에서 월간 랭킹 조회
            List<RankingItem> rankingItems = rankingRepository.getMonthlyTopRankings(yearMonth, size, page);
            log.debug("월간 랭킹 Repository 조회 완료 - yearMonth: {}, 조회된 아이템 수: {}",
                    yearMonth, rankingItems.size());

            // 3. 전체 랭킹 상품 수 조회
            long totalProducts = rankingRepository.getTotalMonthlyRankingCount(yearMonth);
            log.debug("월간 랭킹 전체 상품 수: {}", totalProducts);

            // 4. 상품 정보 일괄 조회 및 결합
            List<RankingInfo.RankingItem> dtoItems = aggregateProductInfo(rankingItems);
            log.debug("월간 랭킹 상품 정보 결합 완료 - 최종 아이템 수: {}", dtoItems.size());

            // 5. 페이징 메타데이터 생성
            RankingInfo.PaginationInfo pagination = RankingInfo.PaginationInfo.of(page, size, totalProducts);
            RankingInfo.RankingMeta meta = RankingInfo.RankingMeta.of(yearMonth, totalProducts);

            // 6. 최종 응답 DTO 생성
            RankingInfo.PageResult response = RankingInfo.PageResult.of(dtoItems, pagination, meta);

            log.info("월간 랭킹 조회 완료 - date: {}, yearMonth: {}, totalProducts: {}, returnedItems: {}",
                    date, yearMonth, totalProducts, dtoItems.size());

            return response;

        } catch (Exception e) {
            log.error("월간 랭킹 조회 실패 - date: {}, size: {}, page: {}, error: {}",
                    date, size, page, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * yyyyMMdd → 년도-주차 변환
     * 예: "20240915" → "2024-38"
     */
    private String convertDateToYearWeek(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return WeeklyProductRanking.getYearWeekOf(localDate);
        } catch (Exception e) {
            log.error("날짜 → 년도-주차 변환 실패 - date: {}", date, e);
            throw new IllegalArgumentException("잘못된 날짜 형식: " + date, e);
        }
    }

    /**
     * yyyyMMdd → 년도-월 변환
     * 예: "20240915" → "2024-09"
     */
    private String convertDateToYearMonth(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return MonthlyProductRanking.getYearMonthOf(localDate);
        } catch (Exception e) {
            log.error("날짜 → 년도-월 변환 실패 - date: {}", date, e);
            throw new IllegalArgumentException("잘못된 날짜 형식: " + date, e);
        }
    }

    /**
     * 랭킹 아이템과 상품 정보를 결합하여 Application DTO 생성
     *
     * 기존 RankingQueryService.aggregateProductInfo()와 동일한 로직
     * 코드 중복이지만 Service 분리를 위해 복사
     */
    private List<RankingInfo.RankingItem> aggregateProductInfo(List<RankingItem> rankingItems) {
        if (rankingItems.isEmpty()) {
            return List.of();
        }

        try {
            // 1. 상품 ID 추출
            List<Long> productIds = rankingItems.stream()
                    .map(RankingItem::productId)
                    .collect(Collectors.toList());

            log.debug("상품 정보 조회 시작 - productIds: {}", productIds);

            // 2. 상품 정보 일괄 조회 (N+1 문제 방지)
            List<Product> products = productRepository.findAllById(productIds);
            log.debug("상품 정보 일괄 조회 완료 - 조회된 상품 수: {}", products.size());

            // 3. 브랜드 ID 추출
            List<Long> brandIds = products.stream()
                    .map(Product::getBrandId)
                    .distinct()
                    .collect(Collectors.toList());

            log.debug("브랜드 정보 조회 시작 - brandIds: {}", brandIds);

            // 4. 브랜드 정보 일괄 조회 (N+1 문제 방지)
            List<Brand> brands = brandRepository.findAllById(brandIds);
            log.debug("브랜드 정보 일괄 조회 완료 - 조회된 브랜드 수: {}", brands.size());

            // 5. ID를 키로 하는 Map 생성 (빠른 매칭용)
            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));

            Map<Long, Brand> brandMap = brands.stream()
                    .collect(Collectors.toMap(Brand::getId, Function.identity()));

            // 6. 랭킹 정보 + 상품 정보 + 브랜드 정보 결합하여 Application DTO 생성
            return rankingItems.stream()
                    .map(rankingItem -> {
                        Product product = productMap.get(rankingItem.productId());

                        if (product == null) {
                            log.warn("상품 정보를 찾을 수 없음 - productId: {}", rankingItem.productId());
                            return null;
                        }

                        Brand brand = brandMap.get(product.getBrandId());
                        String brandName = brand != null ? brand.getName() : "Unknown Brand";

                        if (brand == null) {
                            log.warn("브랜드 정보를 찾을 수 없음 - brandId: {}", product.getBrandId());
                        }

                        // Application Layer의 ProductInfo DTO 생성
                        RankingInfo.ProductInfo productInfo = RankingInfo.ProductInfo.of(
                                product.getId(),
                                product.getName(),
                                product.getPrice(),
                                product.getBrandId(),
                                brandName,
                                product.getLikeCount()
                        );

                        // Application Layer의 RankingItem DTO 생성
                        return RankingInfo.RankingItem.of(
                                rankingItem.rank(),
                                rankingItem.score(),
                                productInfo
                        );
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("상품 정보 결합 실패 - error: {}", e.getMessage(), e);
            throw new RuntimeException("상품 정보 조회 실패", e);
        }
    }

}
