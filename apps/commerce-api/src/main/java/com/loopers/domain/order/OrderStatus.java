package com.loopers.domain.order;

public enum OrderStatus {
    PENDING,    // 주문 대기
    PAID,       // 결제 완료
    SHIPPED,    // 배송 중
    DELIVERED,  // 배송 완료
    CANCELLED   // 주문 취소
}
