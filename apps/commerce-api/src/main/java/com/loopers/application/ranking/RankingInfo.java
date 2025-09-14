package com.loopers.application.ranking;

import java.util.List;

/**
 * 랭킹 Application Layer DTO
 *
 * 역할:
 * - Application Layer 내부에서 사용하는 순수 비즈니스 DTO
 * - Domain 객체와 Interface 계층 사이의 중간 변환 역할
 * - 레이어 의존성 규칙 준수 (Application → Interface 의존성 제거)
 *
 * 구조:
 * - PageResult: 페이징된 랭킹 결과
 * - RankingItem: 개별 랭킹 아이템
 * - ProductInfo: 상품 기본 정보
 * - PaginationInfo: 페이징 메타데이터
 * - RankingMeta: 랭킹 집계 정보
 */
public class RankingInfo {

    /**
     * 랭킹 페이지 결과
     *
     * Application Layer에서 사용하는 페이징 결과
     */
    public record PageResult(
            List<RankingItem> rankings,
            PaginationInfo pagination,
            RankingMeta meta
    ) {
        public static PageResult of(List<RankingItem> rankings, PaginationInfo pagination, RankingMeta meta) {
            return new PageResult(rankings, pagination, meta);
        }
    }

    /**
     * 개별 랭킹 아이템
     *
     * 순위, 점수, 상품 정보를 포함하는 비즈니스 객체
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
     * 랭킹에 필요한 상품 정보 (Application Layer용)
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
     * Application Layer에서 사용하는 페이징 메타데이터
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
     * Application Layer에서 사용하는 랭킹 집계 정보
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
