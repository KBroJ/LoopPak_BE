package com.loopers.domain.payment;

public enum PaymentStatus {

    PENDING,    // 결제 요청 전 (주문 생성 직후)
    PROCESSING, // 결제 진행 중 (PG 요청 성공, 콜백 대기 중)
    SUCCESS,    // 결제 성공 (콜백 완료)
    FAILED,     // 결제 실패
    CANCELLED   // 결제 취소

}
