package com.loopers.domain.payment;

public enum PaymentStatus {

    PENDING,    // 결제 대기 (PG 요청 직후)
    SUCCESS,    // 결제 성공
    FAILED,     // 결제 실패
    CANCELLED   // 결제 취소

}
