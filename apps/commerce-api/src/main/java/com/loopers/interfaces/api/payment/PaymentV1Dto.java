package com.loopers.interfaces.api.payment;


import com.loopers.application.payment.PaymentStatusInfo;

public class PaymentV1Dto {

    public record PaymentStatusResponse(
            String transactionKey,
            Long orderId,
            Long paymentId,
            String ourStatus,        // "PENDING" | "SUCCESS" | "FAILED"
            String pgStatus,         // "SUCCESS" | "FAILED" | "CANCELLED"
            boolean isSync,
            String message
    ) {
        public static PaymentStatusResponse from(PaymentStatusInfo info) {
            return new PaymentStatusResponse(
                    info.transactionKey(),
                    info.orderId(),
                    info.paymentId(),
                    info.ourStatus(),
                    info.pgStatus().name(),
                    info.isSync(),
                    info.message()
            );
        }
    }

    public record PaymentSyncRequest(
            String transactionKey
    ) {
    }

}
