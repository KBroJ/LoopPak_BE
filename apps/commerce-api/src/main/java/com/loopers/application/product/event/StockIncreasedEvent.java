package com.loopers.application.product.event;

import com.loopers.application.product.StockChangeReason;

import java.time.Instant;

/**
 * 재고 증가 이벤트
 *
 * 발행 시점:
 * - 주문 취소로 인해 재고가 복구될 때
 * - 상품 반품으로 재고가 늘어날 때
 * - 신규 입고로 재고가 추가될 때
 *
 * Consumer 처리 내용:
 * - 캐시 무효화: 해당 상품의 상세 정보 캐시 삭제
 * - 집계: product_metrics 테이블 업데이트
 * - 감사 로그: event_log 테이블에 재고 변경 이력 저장
 * - 품절 복구 시: 상품 목록 캐시 삭제 (다시 판매 가능 상품으로 노출)
 */
public record StockIncreasedEvent(
    Long productId,           // 재고가 증가한 상품 ID
    int previousStock,        // 변경 전 재고량
    int currentStock,         // 변경 후 재고량
    int increasedQuantity,    // 증가된 수량
    StockChangeReason reason, // 증가 사유 (CANCEL, RETURN, RESTOCK 등)
    Instant occurredAt        // 이벤트 발생 시각
) {

    // 기본 생성 메서드
    public static StockIncreasedEvent of(
        Long productId, int previousStock, int currentStock, int increasedQuantity, StockChangeReason reason
    ) {
        return new StockIncreasedEvent(
            productId, previousStock, currentStock, increasedQuantity, reason, Instant.now()
        );
    }

    // 주문 취소로 인한 재고 증가용 편의 메서드
    public static StockIncreasedEvent forOrderCancel(
        Long productId, int previousStock, int currentStock, int increasedQuantity
    ) {
        return of(productId, previousStock, currentStock, increasedQuantity, StockChangeReason.CANCEL);
    }

    // 신규 입고용 편의 메서드
    public static StockIncreasedEvent forRestock(
        Long productId, int previousStock, int currentStock, int increasedQuantity
    ) {
        return of(productId, previousStock, currentStock, increasedQuantity, StockChangeReason.RESTOCK);
    }

    // 품절에서 복구 여부 확인 (Consumer에서 캐시 전략 결정할 때 사용)
    public boolean isStockRestored() {
        return previousStock == 0 && currentStock > 0;
    }

}
