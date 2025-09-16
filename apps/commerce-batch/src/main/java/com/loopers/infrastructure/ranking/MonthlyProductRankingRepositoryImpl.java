package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyProductRanking;
import com.loopers.domain.ranking.MonthlyProductRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 월간 상품 랭킹 Repository 구현체 (Infrastructure Layer)
 */
@Component
@RequiredArgsConstructor
public class MonthlyProductRankingRepositoryImpl implements MonthlyProductRankingRepository {

    private final MonthlyProductRankingJpaRepository jpaRepository;

    @Override
    public List<MonthlyProductRanking> saveAll(List<MonthlyProductRanking> rankings) {
        return jpaRepository.saveAll(rankings);
    }

    @Override
    public List<MonthlyProductRanking> findByYearMonthOrderByRankPosition(String yearMonth, Pageable pageable) {
        return jpaRepository.findByYearMonthOrderByRankPosition(yearMonth, pageable);
    }

    @Override
    public List<MonthlyProductRanking> findByYearMonth(String yearMonth) {
        return jpaRepository.findByYearMonth(yearMonth);
    }

    @Override
    public int deleteByYearMonth(String yearMonth) {
        return jpaRepository.deleteByYearMonth(yearMonth);
    }

    @Override
    public String findLatestYearMonth() {
        return jpaRepository.findLatestYearMonth();
    }

    @Override
    public List<String> findDistinctYearMonths() {
        return jpaRepository.findDistinctYearMonths();
    }

    @Override
    public long countByYearMonth(String yearMonth) {
        return jpaRepository.countByYearMonth(yearMonth);
    }

    @Override
    public boolean existsByYearMonthAndProductId(String yearMonth, Long productId) {
        return jpaRepository.existsByYearMonthAndProductId(yearMonth, productId);
    }
    
}
