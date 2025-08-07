package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.coupon.UserCouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository userCouponJpaRepository;

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponJpaRepository.save(userCoupon);
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return userCouponJpaRepository.findById(id);
    }

    @Override
    public Optional<UserCoupon> findByIdAndUserId(Long id, Long userId) {
        return userCouponJpaRepository.findByIdAndUserId(id, userId);
    }

    @Override
    public Page<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable) {
        return userCouponJpaRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    @Override
    public List<UserCoupon> findByStatusAndExpiresAtBefore(UserCouponStatus status, ZonedDateTime now) {
        return userCouponJpaRepository.findByStatusAndExpiresAtBefore(status, now);
    }

}
