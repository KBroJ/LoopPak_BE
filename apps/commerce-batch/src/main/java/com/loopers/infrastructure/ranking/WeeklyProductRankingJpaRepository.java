package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.WeeklyProductRanking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 주간 상품 랭킹 JPA Repository (Infrastructure Layer)
 */
public interface WeeklyProductRankingJpaRepository extends JpaRepository<WeeklyProductRanking, Long> {

    @Query("SELECT w FROM WeeklyProductRanking w WHERE w.yearWeek = :yearWeek ORDER BY w.rankPosition ASC")
    List<WeeklyProductRanking> findByYearWeekOrderByRankPosition(@Param("yearWeek") String yearWeek, Pageable pageable);

    List<WeeklyProductRanking> findByYearWeek(@Param("yearWeek") String yearWeek);

    @Modifying
    @Query("DELETE FROM WeeklyProductRanking w WHERE w.yearWeek = :yearWeek")
    int deleteByYearWeek(@Param("yearWeek") String yearWeek);

    @Query("SELECT MAX(w.yearWeek) FROM WeeklyProductRanking w")
    String findLatestYearWeek();

    @Query("SELECT DISTINCT w.yearWeek FROM WeeklyProductRanking w ORDER BY w.yearWeek DESC")
    List<String> findDistinctYearWeeks();

    long countByYearWeek(@Param("yearWeek") String yearWeek);

    boolean existsByYearWeekAndProductId(@Param("yearWeek") String yearWeek, @Param("productId") Long productId);

}
