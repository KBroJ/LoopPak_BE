package com.loopers.domain.ranking;

import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 월간 상품 랭킹 Repository 인터페이스
 */
public interface MonthlyProductRankingRepository {

    /**
     * 월간 랭킹 저장
     */
    List<MonthlyProductRanking> saveAll(List<MonthlyProductRanking> rankings);

    /**
     * 특정 월의 랭킹 목록 조회 (순위 순)
     */
    List<MonthlyProductRanking> findByYearMonthOrderByRankPosition(String yearMonth, Pageable pageable);

    /**
     * 특정 월의 모든 랭킹 조회
     */
    List<MonthlyProductRanking> findByYearMonth(String yearMonth);

    /**
     * 특정 월의 랭킹 데이터 삭제
     */
    int deleteByYearMonth(String yearMonth);

    /**
     * 가장 최근 월 조회
     */
    String findLatestYearMonth();

    /**
     * 랭킹이 존재하는 월 목록 조회
     */
    List<String> findDistinctYearMonths();

    /**
     * 특정 월의 랭킹 개수 조회
     */
    long countByYearMonth(String yearMonth);

    /**
     * 특정 월에 특정 상품의 랭킹 존재 여부 확인
     */
    boolean existsByYearMonthAndProductId(String yearMonth, Long productId);

}
