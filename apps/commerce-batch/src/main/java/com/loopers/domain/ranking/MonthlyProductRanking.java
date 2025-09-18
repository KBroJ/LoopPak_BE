package com.loopers.domain.ranking;

import com.loopers.application.batch.service.RankingScoreCalculator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 월간 상품 랭킹 엔티티
 * 매월 product_metrics 스냅샷을 기반으로 TOP 100 랭킹 저장
 */
@Entity
@Table(name = "mv_product_rank_monthly")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MonthlyProductRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 년도-월
     * 예: "2024-09"
     */
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    /**
     * 랭킹 순위 (1~100)
     */
    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    /**
     * 스냅샷 시점 좋아요 수
     */
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 스냅샷 시점 조회수
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 스냅샷 시점 판매량
     */
    @Column(name = "sales_count", nullable = false)
    @Builder.Default
    private Integer salesCount = 0;

    /**
     * 랭킹 계산 점수
     * 공식: (like_count * 3.0) + (sales_count * 2.0) + (view_count * 1.0)
     */
    @Column(name = "ranking_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal rankingScore;

    /**
     * 스냅샷 생성 시간
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 현재 월의 yearMonth 문자열 생성
     */
    public static String getCurrentYearMonth() {
        LocalDate now = LocalDate.now();
        return now.getYear() + "-" + String.format("%02d", now.getMonthValue());
    }

    /**
     * 특정 날짜의 yearMonth 문자열 생성
     */
    public static String getYearMonthOf(LocalDate date) {
        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }

    /**
     * 랭킹 점수 계산 및 설정
     * 가중치: application.yml에서 관리
     *  - 기본값 : 좋아요(3) + 판매량(2) + 조회수(1)
     */
    public void calculateRankingScore(RankingScoreCalculator calculator) {
        this.rankingScore = calculator.calculate(likeCount, salesCount, viewCount);
    }

    /**
     * 정적 팩토리 메서드 - ProductMetrics 기반 생성
     */
    public static MonthlyProductRanking createFrom(
            Long productId, String yearMonth, Integer rankPosition,
            Integer likeCount, Integer viewCount, Integer salesCount,
            RankingScoreCalculator calculator
    ) {
        MonthlyProductRanking ranking = MonthlyProductRanking.builder()
                .productId(productId)
                .yearMonth(yearMonth)
                .rankPosition(rankPosition)
                .likeCount(likeCount != null ? likeCount : 0)
                .viewCount(viewCount != null ? viewCount : 0)
                .salesCount(salesCount != null ? salesCount : 0)
                .build();

        ranking.calculateRankingScore(calculator);
        return ranking;
    }

    /**
     * 랭킹 순위 업데이트
     * Writer에서 정렬 후 순위를 부여할 때 사용
     */
    public void updateRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

}
