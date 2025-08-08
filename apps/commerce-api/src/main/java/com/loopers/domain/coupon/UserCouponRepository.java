package com.loopers.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository {

    UserCoupon save(UserCoupon userCoupon);

    Optional<UserCoupon> findById(Long id);

    // 주문 시 특정 쿠폰의 유효성을 검증하기 위해 사용
    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    // [비관적락]주문 시 특정 쿠폰의 유효성을 검증하기 위해 사용
    Optional<UserCoupon> findByIdAndUserIdWithLock(Long id, Long userId);

    // 사용자가 보유한 쿠폰 목록을 조회하기 위해 사용
    Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable);

    // 만료된 쿠폰을 일괄 처리하는 스케줄러 등에서 사용 가능
    List<UserCoupon> findByStatusAndExpiresAtBefore(UserCouponStatus status, ZonedDateTime now);

}
