package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable);

    List<UserCoupon> findByStatusAndExpiresAtBefore(UserCouponStatus status, ZonedDateTime now);

}
