package com.loopers.domain.metrics;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 상품 집계 정보 엔티티
 * 상품별 좋아요, 조회수, 주문수 등을 집계하여 분석에 최적화된 형태로 저장
 */
@Entity
@Table(name = "product_metrics")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductMetrics {

    /**
     * 상품 ID (Primary Key)
     * Product 엔티티의 ID와 동일
     */
    @Id
    @Column(name = "product_id")
    private Long productId;

    /**
     * 누적 좋아요 수
     * LikeAddedEvent/LikeRemovedEvent로 증감
     */
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 누적 조회 수 (향후 확장용)
     * ProductViewedEvent로 증가
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 누적 주문 수 (향후 확장용)
     * OrderCreatedEvent로 증가
     */
    @Column(name = "sales_count", nullable = false)
    @Builder.Default
    private Integer salesCount = 0;

    /**
     * 마지막 업데이트 시간
     * 집계 데이터 신선도 확인용
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 최초 생성 시간
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 좋아요 수 증가
     * LikeAddedEvent 처리 시 호출
     */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     * LikeRemovedEvent 처리 시 호출
     */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /**
     * 주문수 증가
     */
    public void increaseSalesCount() {
        this.salesCount++;
    }

    public void increaseSalesCountBy(int quantity) {
        if (quantity > 0) {
            this.salesCount += quantity;
        }
    }

    /**
     * 새로운 상품 집계 생성을 위한 정적 팩토리 메서드
     */
    public static ProductMetrics create(Long productId) {
        return ProductMetrics.builder()
                .productId(productId)
                .build();
    }

    /**
     * 초기 집계값과 함께 생성하는 정적 팩토리 메서드
     */
    public static ProductMetrics createWith(
        Long productId, Integer likeCount,
        Integer viewCount, Integer salesCount
    ) {
        return ProductMetrics.builder()
                .productId(productId)
                .likeCount(likeCount != null ? likeCount : 0)
                .viewCount(viewCount != null ? viewCount : 0)
                .salesCount(salesCount != null ? salesCount : 0)
                .build();
    }

}
