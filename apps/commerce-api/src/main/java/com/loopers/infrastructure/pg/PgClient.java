package com.loopers.infrastructure.pg;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "pgClient",
    url = "${pg.base-url:http://localhost:8082}",
    configuration = PgClientConfig.class
)
public interface PgClient {

    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgPaymentRequest request
    );

    @GetMapping("/api/v1/payments/{transactionKey}")
    PgPaymentResponse getPayment(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable("transactionKey") String transactionKey
    );

    @GetMapping("/api/v1/payments")
    PgPaymentResponse getPaymentByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam("orderId") String orderId
    );

}
