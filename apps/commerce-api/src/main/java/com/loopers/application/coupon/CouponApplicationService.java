package com.loopers.application.coupon;

import com.loopers.domain.coupon.*;
import com.loopers.domain.users.User;
import com.loopers.domain.users.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponApplicationService {

    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 새로운 쿠폰 템플릿(정책)을 생성
     */
    @Transactional
    public CouponInfo createCoupon(
            String name, String description, CouponType type, int discountValue, int totalQuantity, ZonedDateTime validFrom, ZonedDateTime validUntil
    ) {
        Coupon coupon = Coupon.of(name, description, type, discountValue, totalQuantity, validFrom, validUntil);
        Coupon savedCoupon = couponRepository.save(coupon);
        return CouponInfo.from(savedCoupon);
    }

    /**
     * 특정 사용자에게 쿠폰을 발급
     */
    @Transactional
    public void issueCouponToUser(String userId, Long couponId) {
        // 1. 사용자 존재 여부 확인
        if (!userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        // 2. User Entity 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 3. 쿠폰 템플릿 정보 조회 및 유효성 검증
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 정책을 찾을 수 없습니다."));

        // 4. 사용자 쿠폰 생성 및 저장
        ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(30);
        UserCoupon userCoupon = UserCoupon.of(user.getId(), couponId, expiresAt);
        userCouponRepository.save(userCoupon);
    }

    /**
     * 특정 사용자가 보유한 사용 가능한 쿠폰 목록을 조회
     */
    @Transactional(readOnly = true)
    public Page<UserCouponInfo> getMyAvailableCoupons(Long userId, Pageable pageable) {
        // 1. 사용자의 쿠폰 목록(UserCoupon)을 페이지 단위로 조회
        Page<UserCoupon> userCouponPage = userCouponRepository.findByUserIdAndStatus(userId, UserCouponStatus.AVAILABLE, pageable);
        if (userCouponPage.isEmpty()) {
            return Page.empty();
        }

        // 2. 조회된 UserCoupon들의 원본 쿠폰 ID 목록을 추출
        List<Long> couponIds = userCouponPage.getContent().stream()
                .map(UserCoupon::getCouponId)
                .distinct()
                .toList();

        // 3. 원본 쿠폰(Coupon) 정보들을 단 한 번의 쿼리로 모두 조회 (N+1 문제 방지)
        Map<Long, Coupon> couponMap = couponRepository.findAllById(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, coupon -> coupon));

        // 4. UserCoupon 페이지를 UserCouponInfo 페이지로 변환
        return userCouponPage.map(userCoupon -> {
            Coupon coupon = couponMap.get(userCoupon.getCouponId());
            return UserCouponInfo.of(userCoupon, coupon);
        });
    }

}
