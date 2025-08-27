package com.loopers.domain.coupon;

public enum UserCouponStatus {
    AVAILABLE,  // 사용 가능
    RESERVED,   // 예약 (주문 생성 시점에 잠금)
    USED,       // 사용 완료
    EXPIRED     // 기간 만료
}
