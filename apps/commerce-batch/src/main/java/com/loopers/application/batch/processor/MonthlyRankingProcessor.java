package com.loopers.application.batch.processor;

import com.loopers.application.batch.service.RankingScoreCalculator;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.ranking.MonthlyProductRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * ProductMetrics를 MonthlyProductRanking으로 변환하는 Processor
 *
 * 역할:
 * - ProductMetrics 입력 데이터 검증
 * - 가중치 적용한 랭킹 점수 계산
 * - MonthlyProductRanking 객체로 변환
 * - 유효하지 않은 데이터 필터링 (null 반환)
 *
 * Spring Batch 흐름:
 * Reader → [이 Processor] → Writer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyRankingProcessor implements ItemProcessor<ProductMetrics, MonthlyProductRanking> {

    private final RankingScoreCalculator scoreCalculator;

    @Override
    public MonthlyProductRanking process(ProductMetrics item) throws Exception {

        // 1. 입력 데이터 검증
        if (item == null || item.getProductId() == null) {
            log.warn("유효하지 않은 ProductMetrics 데이터: {}", item);
            return null; // null 반환 시 Writer로 전달되지 않음
        }

        // 2. 현재 월 정보 생성
        String yearMonth = MonthlyProductRanking.getCurrentYearMonth();

        // 3. MonthlyProductRanking 객체 생성
        MonthlyProductRanking ranking = MonthlyProductRanking.createFrom(
                item.getProductId(),    // productId
                yearMonth,              // yearMonth
                null,                   // rankPosition (아직 순위 미결정, Writer에서 처리)
                item.getLikeCount(),    // likeCount
                item.getViewCount(),    // viewCount
                item.getSalesCount(),   // salesCount
                scoreCalculator         // RankingScoreCalculator
        );

        log.debug("ProductMetrics 변환 완료 (월간): productId={}, score={}",
                item.getProductId(), ranking.getRankingScore());

        return ranking;

    }

}
