package com.loopers.application.batch.service;

import com.loopers.application.batch.config.RankingWeightConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 랭킹 점수 계산 서비스
 * 가중치 설정을 기반으로 상품별 랭킹 점수를 계산
 *
 * 계산 공식: (like_count * like_weight) + (sales_count * sales_weight) + (view_count * view_weight)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScoreCalculator {

    private final RankingWeightConfig weightConfig;

    /**
     * 랭킹 점수 계산
     * null 값에 대한 안전 처리 포함
     *
     * @param likeCount 좋아요 수
     * @param salesCount 판매량
     * @param viewCount 조회수
     * @return 계산된 랭킹 점수
     */
    public BigDecimal calculate(Integer likeCount, Integer salesCount, Integer viewCount) {
        // null 안전 처리
        int safeLikeCount = likeCount != null ? likeCount : 0;
        int safeSalesCount = salesCount != null ? salesCount : 0;
        int safeViewCount = viewCount != null ? viewCount : 0;

        // 가중치 적용 계산
        BigDecimal likeScore = BigDecimal.valueOf(safeLikeCount * weightConfig.getLike());
        BigDecimal salesScore = BigDecimal.valueOf(safeSalesCount * weightConfig.getSales());
        BigDecimal viewScore = BigDecimal.valueOf(safeViewCount * weightConfig.getView());

        BigDecimal totalScore = likeScore.add(salesScore).add(viewScore);

        log.debug("랭킹 점수 계산: 좋아요({}*{}) + 판매량({}*{}) + 조회수({}*{}) = {}",
                safeLikeCount, weightConfig.getLike(),
                safeSalesCount, weightConfig.getSales(),
                safeViewCount, weightConfig.getView(),
                totalScore);

        return totalScore;
    }

    /**
     * 현재 가중치 설정 정보 반환 (디버깅/로깅용)
     */
    public String getWeightInfo() {
        return String.format("좋아요:%.1f, 판매량:%.1f, 조회수:%.1f",
                weightConfig.getLike(), weightConfig.getSales(), weightConfig.getView());
    }

    /**
     * 가중치 설정이 유효한지 검증
     */
    public boolean isValidWeight() {
        return weightConfig.getLike() >= 0 &&
                weightConfig.getSales() >= 0 &&
                weightConfig.getView() >= 0;
    }

}
