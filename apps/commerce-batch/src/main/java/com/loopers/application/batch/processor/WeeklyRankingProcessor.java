package com.loopers.application.batch.processor;

import com.loopers.application.batch.service.RankingScoreCalculator;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.ranking.WeeklyProductRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ProductMetrics를 WeeklyProductRanking으로 변환하는 Processor
 *
 * 역할:
 * - ProductMetrics 입력 데이터 검증
 * - 가중치 적용한 랭킹 점수 계산
 * - WeeklyProductRanking 객체로 변환
 * - 유효하지 않은 데이터 필터링 (null 반환)
 *
 * Spring Batch 흐름:
 * Reader → [이 Processor] → Writer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyRankingProcessor implements ItemProcessor<ProductMetrics, WeeklyProductRanking> {

    private final RankingScoreCalculator scoreCalculator;

    @Override
    public WeeklyProductRanking process(ProductMetrics item) throws Exception {

        // 1. 입력 데이터 검증
        if (item == null || item.getProductId() == null) {
            log.warn("유효하지 않은 ProductMetrics 데이터: {}", item);
            return null; // null 반환 시 Writer로 전달되지 않음
        }

        // 2. 현재 주차 정보 생성
        String yearWeek = WeeklyProductRanking.getCurrentYearWeek();

        // 3. WeeklyProductRanking 객체 생성 (올바른 메서드 사용)
        WeeklyProductRanking ranking = WeeklyProductRanking.createFrom(
                item.getProductId(),    // productId
                yearWeek,               // yearWeek
                null,                   // rankPosition (아직 순위 미결정, Writer에서 처리)
                item.getLikeCount(),    // likeCount
                item.getViewCount(),    // viewCount
                item.getSalesCount(),   // salesCount
                scoreCalculator         // RankingScoreCalculator
        );

        log.debug("ProductMetrics 변환 완료: productId={}, score={}", item.getProductId(), ranking.getRankingScore());

        return ranking;
    }

}
