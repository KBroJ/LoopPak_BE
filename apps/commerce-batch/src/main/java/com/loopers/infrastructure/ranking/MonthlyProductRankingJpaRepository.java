package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyProductRanking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 월간 상품 랭킹 JPA Repository (Infrastructure Layer)
 */
public interface MonthlyProductRankingJpaRepository extends JpaRepository<MonthlyProductRanking, Long> {

    @Query("SELECT m FROM MonthlyProductRanking m WHERE m.yearMonth = :yearMonth ORDER BY m.rankPosition ASC")
    List<MonthlyProductRanking> findByYearMonthOrderByRankPosition(@Param("yearMonth") String yearMonth, Pageable pageable);

    List<MonthlyProductRanking> findByYearMonth(@Param("yearMonth") String yearMonth);

    @Modifying
    @Query("DELETE FROM MonthlyProductRanking m WHERE m.yearMonth = :yearMonth")
    int deleteByYearMonth(@Param("yearMonth") String yearMonth);

    @Query("SELECT MAX(m.yearMonth) FROM MonthlyProductRanking m")
    String findLatestYearMonth();

    @Query("SELECT DISTINCT m.yearMonth FROM MonthlyProductRanking m ORDER BY m.yearMonth DESC")
    List<String> findDistinctYearMonths();

    long countByYearMonth(@Param("yearMonth") String yearMonth);

    boolean existsByYearMonthAndProductId(@Param("yearMonth") String yearMonth, @Param("productId") Long productId);

}
