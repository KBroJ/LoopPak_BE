package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.WeeklyProductRanking;
import com.loopers.domain.ranking.WeeklyProductRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 주간 상품 랭킹 Repository 구현체 (Infrastructure Layer)
 */
@Component
@RequiredArgsConstructor
public class WeeklyProductRankingRepositoryImpl implements WeeklyProductRankingRepository {

    private final WeeklyProductRankingJpaRepository jpaRepository;

    @Override
    public List<WeeklyProductRanking> saveAll(List<WeeklyProductRanking> rankings) {
        return jpaRepository.saveAll(rankings);
    }

    @Override
    public List<WeeklyProductRanking> findByYearWeekOrderByRankPosition(String yearWeek, Pageable pageable) {
        return jpaRepository.findByYearWeekOrderByRankPosition(yearWeek, pageable);
    }

    @Override
    public List<WeeklyProductRanking> findByYearWeek(String yearWeek) {
        return jpaRepository.findByYearWeek(yearWeek);
    }

    @Override
    public int deleteByYearWeek(String yearWeek) {
        return jpaRepository.deleteByYearWeek(yearWeek);
    }

    @Override
    public String findLatestYearWeek() {
        return jpaRepository.findLatestYearWeek();
    }

    @Override
    public List<String> findDistinctYearWeeks() {
        return jpaRepository.findDistinctYearWeeks();
    }

    @Override
    public long countByYearWeek(String yearWeek) {
        return jpaRepository.countByYearWeek(yearWeek);
    }

    @Override
    public boolean existsByYearWeekAndProductId(String yearWeek, Long productId) {
        return jpaRepository.existsByYearWeekAndProductId(yearWeek, productId);
    }

}
