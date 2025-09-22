package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyProductRanking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyProductRankingJpaRepository extends JpaRepository<MonthlyProductRanking, Long> {

    @Query("SELECT m FROM MonthlyProductRanking m " +
            "WHERE m.yearMonth = :yearMonth " +
            "ORDER BY m.rankPosition ASC")
    Page<MonthlyProductRanking> findByYearMonthOrderByRankPositionAsc(
            @Param("yearMonth") String yearMonth,
            Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM MonthlyProductRanking m WHERE m.yearMonth = :yearMonth")
    long countByYearMonth(@Param("yearMonth") String yearMonth);

}
