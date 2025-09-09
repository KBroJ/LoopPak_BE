package com.loopers.domain.product;

/**
 * 재고 변경 결과를 담는 Value Object
 *
 * 목적:
 * - Domain Entity(Product)에서 Application Layer로 재고 변경 정보 전달
 * - Application Layer에서 이 정보를 바탕으로 이벤트 생성 및 발행
 */
public record StockChangeResult(
    Long productId,                 // 재고가 변경된 상품 ID
    int previousStock,              // 변경 전 재고량
    int currentStock,               // 변경 후 재고량
    int changedQuantity,            // 변경된 수량 (절대값)
    StockChangeType changeType      // 변경 타입: DECREASED 또는 INCREASED
) {

    // 재고 증가 결과 생성
    public static StockChangeResult increased(
        Long productId, int previousStock, int currentStock, int increasedQuantity
    ) {
        return new StockChangeResult(
            productId, previousStock, currentStock, increasedQuantity, StockChangeType.INCREASED);
    }

    // 재고 감소 결과 생성
    public static StockChangeResult decreased(
        Long productId, int previousStock, int currentStock, int decreasedQuantity
    ) {
        return new StockChangeResult(
            productId, previousStock, currentStock, decreasedQuantity, StockChangeType.DECREASED
        );
    }

    // 재고 증가인지 확인
    public boolean isIncrease() {
        return changeType.isIncrease();
    }

    // 재고 감소인지 확인
    public boolean isDecrease() {
        return changeType.isDecrease();
    }

    // 재고 변경 여부 확인
    public boolean isStockChanged() {
        return changedQuantity > 0;
    }

    // 재고 소진 여부 확인 (캐시 전략 결정용)
    public boolean isStockDepleted() {
        return changeType.isDecrease() && currentStock == 0;
    }

    // 재고 복구 여부 확인 (품절 → 재고 있음)
    public boolean isStockRestored() {
        return changeType.isIncrease() && previousStock == 0 && currentStock > 0;
    }




}
