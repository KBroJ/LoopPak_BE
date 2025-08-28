package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentStatus;

public record PaymentResult(
    boolean success,
    PaymentStatus status,
    String message,
    String transactionId
) {

    public static PaymentResult pointSuccess() {
        return new PaymentResult(true, PaymentStatus.SUCCESS, "포인트 결제 완료", null);
    }

    public static PaymentResult cardRequestSuccess(String transactionId) {
        return new PaymentResult(true, PaymentStatus.PROCESSING, "카드 결제 요청 완료",
                transactionId);
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, PaymentStatus.FAILED, message, null);
    }

    public static PaymentResult fallback(String transactionId, String message) {
        return new PaymentResult(false, PaymentStatus.PENDING, message, transactionId);
    }

}
