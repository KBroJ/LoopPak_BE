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



    /**
     * 주간 랭킹 조회 (Materialized View 기반)
     *
     * 처리 방식:
     * - Redis 대신 DB의 mv_product_rank_weekly 테이블 조회
     * - Infrastructure에서 JPA로 구현
     *
     * @param yearWeek 년도-주차 (예: "2024-38")
     * @param size 페이지 크기
     * @param page 페이지 번호 (0부터 시작)
     * @return 주간 랭킹 아이템 목록 (기존 RankingItem과 동일한 구조)
     */
    List<RankingItem> getWeeklyTopRankings(String yearWeek, int size, int page);

    /**
     * 주간 랭킹 전체 수 조회
     *
     * 용도: 페이징 메타데이터 생성 (전체 페이지 수 계산)
     *
     * @param yearWeek 년도-주차 (예: "2024-38")
     * @return 해당 주차의 전체 랭킹 상품 수
     */
    long getTotalWeeklyRankingCount(String yearWeek);

    /**
     * 월간 랭킹 조회 (Materialized View 기반)
     *
     * 처리 방식:
     * - Redis 대신 DB의 mv_product_rank_monthly 테이블 조회
     * - Infrastructure에서 JPA로 구현
     *
     * @param yearMonth 년도-월 (예: "2024-09")
     * @param size 페이지 크기
     * @param page 페이지 번호 (0부터 시작)
     * @return 월간 랭킹 아이템 목록 (기존 RankingItem과 동일한 구조)
     */
    List<RankingItem> getMonthlyTopRankings(String yearMonth, int size, int page);

    /**
     * 월간 랭킹 전체 수 조회
     *
     * 용도: 페이징 메타데이터 생성 (전체 페이지 수 계산)
     *
     * @param yearMonth 년도-월 (예: "2024-09")
     * @return 해당 월의 전체 랭킹 상품 수
     */
    long getTotalMonthlyRankingCount(String yearMonth);

}
