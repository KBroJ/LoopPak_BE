package com.loopers.domain.ranking;

import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 주간 상품 랭킹 Repository 인터페이스
 */
public interface WeeklyProductRankingRepository {

    /**
     * 주간 랭킹 저장
     */
    List<WeeklyProductRanking> saveAll(List<WeeklyProductRanking> rankings);

    /**
     * 특정 주차의 랭킹 목록 조회 (순위 순)
     */
    List<WeeklyProductRanking> findByYearWeekOrderByRankPosition(String yearWeek, Pageable pageable);

    /**
     * 특정 주차의 모든 랭킹 조회
     */
    List<WeeklyProductRanking> findByYearWeek(String yearWeek);

    /**
     * 특정 주차의 랭킹 데이터 삭제
     */
    int deleteByYearWeek(String yearWeek);

    /**
     * 가장 최근 주차 조회
     */
    String findLatestYearWeek();

    /**
     * 랭킹이 존재하는 주차 목록 조회
     */
    List<String> findDistinctYearWeeks();

    /**
     * 특정 주차의 랭킹 개수 조회
     */
    long countByYearWeek(String yearWeek);

    /**
     * 특정 주차에 특정 상품의 랭킹 존재 여부 확인
     */
    boolean existsByYearWeekAndProductId(String yearWeek, Long productId);

}
