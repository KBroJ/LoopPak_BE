package com.loopers.application.order;

public record CardInfo(
    String cardType,    // "SAMSUNG", "HYUNDAI" 등
    String cardNo       // "1234-5678-9012-3456"
) {
}
