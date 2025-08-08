package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select uc from UserCoupon uc where uc.id = :id and uc.userId = :userId")
    Optional<UserCoupon> findByIdAndUserIdWithLock(@Param("id") Long id, @Param("userId") Long userId);

    List<UserCoupon> findByStatusAndExpiresAtBefore(UserCouponStatus status, ZonedDateTime now);

}
