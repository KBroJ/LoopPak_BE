package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.WeeklyProductRanking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyProductRankingJpaRepository extends JpaRepository<WeeklyProductRanking, Long> {

    @Query("SELECT w FROM WeeklyProductRanking w " +
            "WHERE w.yearWeek = :yearWeek " +
            "ORDER BY w.rankPosition ASC")
    Page<WeeklyProductRanking> findByYearWeekOrderByRankPositionAsc(
            @Param("yearWeek") String yearWeek,
            Pageable pageable
    );

    @Query("SELECT COUNT(w) FROM WeeklyProductRanking w WHERE w.yearWeek = :yearWeek")
    long countByYearWeek(@Param("yearWeek") String yearWeek);

}
