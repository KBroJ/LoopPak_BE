package com.loopers.interfaces.api.ranking;

import java.util.List;

/**
 * 랭킹 API DTO
 *
 * 구조:
 * - PageResponse: 페이징된 랭킹 목록
 * - RankingItem: 개별 랭킹 정보 (순위 + 상품 정보)
 */
public class RankingV1Dto {

    /**
     * 랭킹 페이지 응답
     *
     * 포함 정보:
     * - rankings: 랭킹 목록
     * - pagination: 페이징 정보
     * - meta: 랭킹 메타데이터
     */
    public record PageResponse(
            List<RankingItem> rankings,
            PaginationInfo pagination,
            RankingMeta meta
    ) {
        public static PageResponse of(List<RankingItem> rankings, PaginationInfo pagination, RankingMeta meta) {
            return new PageResponse(rankings, pagination, meta);
        }
    }

    /**
     * 개별 랭킹 아이템
     *
     * 포함 정보:
     * - rank: 순위 (1위부터)
     * - score: 랭킹 점수
     * - product: 상품 정보
     */
    public record RankingItem(
            int rank,
            double score,
            ProductInfo product
    ) {
        public static RankingItem of(int rank, double score, ProductInfo product) {
            return new RankingItem(rank, score, product);
        }
    }

    /**
     * 상품 기본 정보
     *
     * 랭킹에 필요한 상품 정보만 포함
     */
    public record ProductInfo(
            Long id,
            String name,
            long price,
            Long brandId,
            String brandName,
            long likeCount
    ) {
        public static ProductInfo of(Long id, String name, long price, Long brandId, String brandName, long
                likeCount) {
            return new ProductInfo(id, name, price, brandId, brandName, likeCount);
        }
    }

    /**
     * 페이징 정보
     *
     * 페이지네이션 메타데이터
     */
    public record PaginationInfo(
            int currentPage,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public static PaginationInfo of(int currentPage, int size, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            boolean hasNext = currentPage < totalPages - 1;
            boolean hasPrevious = currentPage > 0;

            return new PaginationInfo(currentPage, size, totalElements, totalPages, hasNext, hasPrevious);
        }
    }

    /**
     * 랭킹 메타데이터
     *
     * 랭킹 집계 정보
     */
    public record RankingMeta(
            String date,
            String description,
            long totalProducts
    ) {
        public static RankingMeta of(String date, long totalProducts) {
            String description = String.format("%s일 상품 랭킹 (총 %d개 상품)", date, totalProducts);
            return new RankingMeta(date, description, totalProducts);
        }
    }

}
