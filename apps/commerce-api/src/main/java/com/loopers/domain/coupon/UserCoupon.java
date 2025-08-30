package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Getter
@Table(name = "user_coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCouponStatus status;

    private ZonedDateTime usedAt;

    @Column(nullable = false)
    private ZonedDateTime expiresAt;

    private UserCoupon(Long userId, Long couponId, ZonedDateTime expiresAt) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = UserCouponStatus.AVAILABLE;
        this.expiresAt = expiresAt;
    }

    public static UserCoupon of(Long userId, Long couponId, ZonedDateTime expiresAt) {
        return new UserCoupon(userId, couponId, expiresAt);
    }

    /**
     * 쿠폰 사용 처리
     */
    public void use() {
        if (this.status != UserCouponStatus.AVAILABLE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용했거나 만료된 쿠폰입니다.");
        }
        if (ZonedDateTime.now().isAfter(this.expiresAt)) {
            this.status = UserCouponStatus.EXPIRED; // 혹시 모를 스케줄러 오류에 대비한 방어 코드
            throw new CoreException(ErrorType.BAD_REQUEST, "기간이 만료된 쿠폰입니다.");
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = ZonedDateTime.now();
    }

    /**
     * 쿠폰 복구 처리 (주문 취소/결제 실패 시)
     */
    public void restore() {
        if (this.status != UserCouponStatus.USED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용된 쿠폰만 복구할 수 있습니다.");
        }
        if (ZonedDateTime.now().isAfter(this.expiresAt)) {
            this.status = UserCouponStatus.EXPIRED;
            throw new CoreException(ErrorType.BAD_REQUEST, "만료된 쿠폰은 복구할 수 없습니다.");
        }
        this.status = UserCouponStatus.AVAILABLE;
        this.usedAt = null;
    }

    /**
     * 쿠폰 예약 처리 (주문 생성 시)
     */
    public void reserve() {
        if (this.status != UserCouponStatus.AVAILABLE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용했거나 만료된 쿠폰입니다.");
        }
        if (ZonedDateTime.now().isAfter(this.expiresAt)) {
            this.status = UserCouponStatus.EXPIRED;
            throw new CoreException(ErrorType.BAD_REQUEST, "기간이 만료된 쿠폰입니다.");
        }
        this.status = UserCouponStatus.RESERVED;
    }

    /**
     * 쿠폰 사용 확정 (결제 완료 시)
     */
    public void confirmUsage() {
        if (this.status != UserCouponStatus.RESERVED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "예약된 쿠폰만 사용 확정할 수 있습니다.");
        }
        this.status = UserCouponStatus.USED;
        this.usedAt = ZonedDateTime.now();
    }

    /**
     * 쿠폰 예약 취소 (주문 실패 시)
     */
    public void cancelReservation() {
        if (this.status != UserCouponStatus.RESERVED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "예약된 쿠폰만 취소할 수 있습니다.");
        }
        this.status = UserCouponStatus.AVAILABLE;
    }

    /**
     * 쿠폰 사용 가능 여부 확인 (수정)
     */
    public boolean isUsable() {
        return this.status == UserCouponStatus.AVAILABLE &&
                ZonedDateTime.now().isBefore(this.expiresAt);
    }

}
