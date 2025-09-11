package com.loopers.domain.ranking;

import java.util.List;

/**
 * 랭킹 Repository 인터페이스
 *
 * 역할:
 * - 랭킹 데이터 조회를 위한 도메인 계약 정의
 * - Infrastructure Layer에서 Redis 구현체 제공
 * - 도메인 관점에서 필요한 랭킹 연산만 정의
 *
 * 제공 기능:
 * - Top-N 랭킹 페이징 조회
 * - 전체 랭킹 상품 수 조회
 * - 특정 상품의 순위 조회
 *
 * Redis ZSET 전략:
 * - Key: ranking:all:{yyyyMMdd}
 * - Member: product:{productId}
 * - Score: 가중치 누적 점수
 */
public interface RankingRepository {

    /**
     * Top-N 랭킹 페이징 조회
     *
     * Redis 연산: ZREVRANGE (높은 점수부터 내림차순)
     *
     * @param date 조회 날짜 (yyyyMMdd 형식)
     * @param size 페이지 크기
     * @param page 페이지 번호 (0부터 시작)
     * @return 페이징된 랭킹 아이템 목록
     */
    List<RankingItem> getTopRankings(String date, int size, int page);

    /**
     * 특정 날짜의 전체 랭킹 상품 수 조회
     *
     * Redis 연산: ZCARD (집합 크기)
     *
     * @param date 조회 날짜 (yyyyMMdd 형식)
     * @return 랭킹에 포함된 총 상품 수
     */
    long getTotalRankingCount(String date);

    /**
     * 특정 상품의 현재 순위 조회
     *
     * Redis 연산: ZREVRANK (역순위 조회)
     *
     * @param date 조회 날짜 (yyyyMMdd 형식)
     * @param productId 상품 ID
     * @return 상품의 순위 (1부터 시작, 순위에 없으면 null)
     */
    Integer getProductRank(String date, Long productId);

    /**
     * 특정 상품의 현재 점수 조회
     *
     * Redis 연산: ZSCORE (점수 조회)
     *
     * @param date 조회 날짜 (yyyyMMdd 형식)
     * @param productId 상품 ID
     * @return 상품의 점수 (점수가 없으면 null)
     */
    Double getProductScore(String date, Long productId);

}
