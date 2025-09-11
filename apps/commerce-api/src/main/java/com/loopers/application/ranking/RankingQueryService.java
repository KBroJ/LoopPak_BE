package com.loopers.application.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 랭킹 조회 서비스 (Application Layer)
 *
 * 역할:
 * - Redis ZSET에서 랭킹 데이터 조회
 * - 상품 정보 Aggregation (ID → 상품 상세 정보)
 * - 페이징 처리 및 응답 데이터 변환
 * - 레이어 의존성 준수: Domain Repository 사용, RankingInfo 반환
 *
 * 처리 흐름:
 * 1. Redis ZSET에서 Top-N 랭킹 조회 (ZREVRANGE)
 * 2. 상품 ID 추출 후 상품 정보 일괄 조회
 * 3. 랭킹 + 상품 정보 결합
 * 4. 페이징 메타데이터 생성
 * 5. RankingInfo 변환 후 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingQueryService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final RankingRepository rankingRepository;

    /**
     * 랭킹 페이지 조회
     *
     * 처리 흐름:
     * 1. Redis ZSET 페이징 조회
     * 2. 전체 랭킹 수 조회 (페이징 메타데이터용)
     * 3. 상품 정보 일괄 조회 및 결합
     * 4. RankingInfo 응답 DTO 생성
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param size 페이지 크기
     * @param page 페이지 번호 (0부터 시작)
     * @return 페이징된 랭킹 응답 (Application Layer DTO)
     */
    public RankingInfo.PageResult getRankings(String date, int size, int page) {
        try {
            log.debug("랭킹 조회 시작 - date: {}, size: {}, page: {}", date, size, page);

            // 1. Redis ZSET에서 페이징된 랭킹 조회
            List<RankingItem> rankingItems = rankingRepository.getTopRankings(date, size, page);
            log.debug("Redis에서 랭킹 조회 완료 - 조회된 아이템 수: {}", rankingItems.size());

            // 2. 전체 랭킹 상품 수 조회 (페이징 메타데이터용)
            long totalProducts = rankingRepository.getTotalRankingCount(date);
            log.debug("전체 랭킹 상품 수: {}", totalProducts);

            // 3. 상품 정보 일괄 조회 및 결합
            List<RankingInfo.RankingItem> dtoItems = aggregateProductInfo(rankingItems);
            log.debug("상품 정보 결합 완료 - 최종 아이템 수: {}", dtoItems.size());

            // 4. 페이징 메타데이터 생성
            RankingInfo.PaginationInfo pagination = RankingInfo.PaginationInfo.of(page, size, totalProducts);
            RankingInfo.RankingMeta meta = RankingInfo.RankingMeta.of(date, totalProducts);

            // 5. 최종 응답 DTO 생성 (Application Layer)
            RankingInfo.PageResult response = RankingInfo.PageResult.of(dtoItems, pagination, meta);

            log.info("랭킹 조회 완료 - date: {}, totalProducts: {}, returnedItems: {}",
                    date, totalProducts, dtoItems.size());

            return response;

        } catch (Exception e) {
            log.error("랭킹 조회 실패 - date: {}, size: {}, page: {}, error: {}",
                    date, size, page, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 랭킹 아이템과 상품 정보를 결합하여 Application DTO 생성
     *
     * 최적화 전략:
     * 1. 상품 ID 일괄 추출
     * 2. IN 쿼리로 상품 정보 한번에 조회 (N+1 문제 방지)
     * 3. 브랜드 ID 추출하여 브랜드 정보도 일괄 조회
     * 4. Map으로 빠른 매칭
     * 5. 순위 정보와 결합
     *
     * @param rankingItems Redis에서 조회한 랭킹 아이템 목록 (Domain 객체)
     * @return 상품 정보가 포함된 Application DTO 목록
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
                    .distinct()  // 중복 제거
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
                            return null; // null인 항목은 후에 필터링됨
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
                                product.getLikeCount()  // Product에서 직접 가져오기
                        );

                        // Application Layer의 RankingItem DTO 생성
                        return RankingInfo.RankingItem.of(
                                rankingItem.rank(),
                                rankingItem.score(),
                                productInfo
                        );
                    })
                    .filter(item -> item != null) // null인 항목 제거
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("상품 정보 결합 실패 - error: {}", e.getMessage(), e);
            throw new RuntimeException("상품 정보 조회 실패", e);
        }
    }

}
