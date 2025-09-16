package com.loopers.domain.ranking;

import com.loopers.application.batch.service.RankingScoreCalculator;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * 주간 상품 랭킹 엔티티
 * 매주 product_metrics 스냅샷을 기반으로 TOP 100 랭킹 저장
 */
@Entity
@Table(name = "mv_product_rank_weekly")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WeeklyProductRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 년도-주차 (한국식: 일요일~토요일)
     * 예: "2024-38"
     */
    @Column(name = "year_week", nullable = false, length = 7)
    private String yearWeek;

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
     * 현재 주차의 yearWeek 문자열 생성
     * 한국식 주차 계산 (일요일~토요일)
     */
    public static String getCurrentYearWeek() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int week = now.get(WeekFields.of(Locale.KOREA).weekOfYear());
        return year + "-" + String.format("%02d", week);
    }

    /**
     * 특정 날짜의 yearWeek 문자열 생성
     */
    public static String getYearWeekOf(LocalDate date) {
        int year = date.getYear();
        int week = date.get(WeekFields.of(Locale.KOREA).weekOfYear());
        return year + "-" + String.format("%02d", week);
    }

    /**
     * 랭킹 점수 계산 및 설정
     * 가중치: application.yml에서 관리
     *  - 기본값 : 좋아요(3) + 판매량(2) + 조회수(1)
     */
    // 새로운 Calculator 사용 메서드로 교체
    public void calculateRankingScore(RankingScoreCalculator calculator) {
        this.rankingScore = calculator.calculate(likeCount, salesCount, viewCount);
    }

    /**
     * 정적 팩토리 메서드 - ProductMetrics 기반 생성
     */
    public static WeeklyProductRanking createFrom(
            Long productId, String yearWeek, Integer rankPosition,
            Integer likeCount, Integer viewCount, Integer salesCount,
            RankingScoreCalculator calculator
    ) {
        WeeklyProductRanking ranking = WeeklyProductRanking.builder()
                .productId(productId)
                .yearWeek(yearWeek)
                .rankPosition(rankPosition)
                .likeCount(likeCount != null ? likeCount : 0)
                .viewCount(viewCount != null ? viewCount : 0)
                .salesCount(salesCount != null ? salesCount : 0)
                .build();

        ranking.calculateRankingScore(calculator);
        return ranking;
    }

}
