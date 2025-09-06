package com.loopers.domain.product;

public enum StockChangeType {

    DECREASED("재고 감소"),
    INCREASED("재고 증가");

    private final String description;

    StockChangeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // 재고 감소 타입인지 확인
    public boolean isDecrease() {
        return this == DECREASED;
    }

    // 재고 증가 타입인지 확인
    public boolean isIncrease() {
        return this == INCREASED;
    }

}
