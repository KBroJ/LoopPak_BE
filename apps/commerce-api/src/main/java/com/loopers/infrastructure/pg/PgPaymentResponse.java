package com.loopers.infrastructure.pg;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PgPaymentResponse(
    @JsonProperty("meta")
    Meta meta,

    @JsonProperty("data")
    Data data
) {
    public record Meta(
            @JsonProperty("result")
            String result
    ) {}

    public record Data(
            @JsonProperty("transactionKey")
            String transactionKey,

            @JsonProperty("status")
            String status
    ) {}

    public boolean isSuccess() {
        return meta != null && "SUCCESS".equals(meta.result());
    }

    public String getTransactionKey() {
        return data != null ? data.transactionKey() : null;
    }

    public String getStatus() {
        return data != null ? data.status() : null;
    }

    public static PgPaymentResponse success(String transactionKey, String status) {
        return new PgPaymentResponse(
                new Meta("SUCCESS"),
                new Data(transactionKey, status)
        );
    }

    public static PgPaymentResponse failure(String status) {
        return new PgPaymentResponse(
                new Meta("FAILED"),
                new Data(null, status)
        );
    }
}
