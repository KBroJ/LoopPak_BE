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
}
