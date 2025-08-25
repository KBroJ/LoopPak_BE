package com.loopers.domain.payment;

public enum PaymentType {
    POINT("포인트 결제"),
    CARD("카드 결제");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
