package com.loopers.infrastructure.pg;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PgPaymentRequest(
    @JsonProperty("orderId")
    String orderId,

    @JsonProperty("cardType")
    String cardType,

    @JsonProperty("cardNo")
    String cardNo,

    @JsonProperty("amount")
    String amount,

    @JsonProperty("callbackUrl")
    String callbackUrl
) {
    public static PgPaymentRequest of(
        Long orderId, String cardType, String cardNo,
        long amount, String callbackUrl
    ) {
        return new PgPaymentRequest(
                String.format("%06d", orderId),
                cardType,
                cardNo,
                String.valueOf(amount),
                callbackUrl
        );
    }
}
