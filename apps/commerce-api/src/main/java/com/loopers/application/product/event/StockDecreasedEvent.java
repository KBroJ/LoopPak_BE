package com.loopers.application.product.event;

import com.loopers.application.product.StockChangeReason;

import java.time.Instant;

/**
 * 재고 감소 이벤트
 *
 * 발행 시점:
 * - 주문 시 상품 재고가 차감될 때
 * - 상품 손실, 파손 등으로 재고가 줄어들 때
 *
 * Consumer 처리 내용:
 * - 캐시 무효화: 해당 상품의 상세 정보 캐시 삭제
 * - 집계: product_metrics 테이블의 sales_count 증가
 * - 감사 로그: event_log 테이블에 재고 변경 이력 저장
 * - 재고 소진 시: 상품 목록 캐시도 삭제 (품절 상품 필터링)
 */
public record StockDecreasedEvent(
    Long productId,           // 재고가 감소한 상품 ID
    int previousStock,        // 변경 전 재고량
    int currentStock,         // 변경 후 재고량
    int decreasedQuantity,    // 감소된 수량
    StockChangeReason reason, // 감소 사유 (ORDER, DAMAGE, LOSS 등)
    Instant occurredAt        // 이벤트 발생 시각
) {

    // 기본 생성 메서드
    public static StockDecreasedEvent of(
        Long productId, int previousStock, int currentStock, int decreasedQuantity, StockChangeReason reason
    ) {
        return new StockDecreasedEvent(
            productId, previousStock, currentStock, decreasedQuantity, reason, Instant.now()
        );
    }

    // 주문으로 인한 재고 감소용 편의 메서드
    public static StockDecreasedEvent forOrder(
        Long productId, int previousStock, int currentStock, int decreasedQuantity
    ) {
        return of(productId, previousStock, currentStock, decreasedQuantity, StockChangeReason.ORDER);
    }

    // 재고 소진 여부 확인 (Consumer에서 캐시 무효화 전략 결정할 때 사용)
    public boolean isStockDepleted() {
        return currentStock == 0;
    }

}
