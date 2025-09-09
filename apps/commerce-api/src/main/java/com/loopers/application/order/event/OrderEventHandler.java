package com.loopers.application.order.event;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.users.User;
import com.loopers.domain.users.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 관련 이벤트 처리 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    /**
     * 주문 생성 후 쿠폰 사용 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) // 주문 트랜잭션 커밋 전
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 처리 시작 - 쿠폰 사용 처리 - orderId: {}", event.orderId());

        // === 오직 쿠폰 사용 처리만 ===
        if (event.couponId() != null) {
            // String userId를 Long으로 변환
            User user = userRepository.findByUserId(event.userId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
            Long userInternalId = user.getId();
            
            processCouponUsage(event.couponId(), userInternalId); // 예외 발생 시 트랜잭션 롤백!
            log.info("쿠폰 사용 처리 완료 - orderId: {}, couponId: {}", event.orderId(), event.couponId());
        } else {
            log.info("쿠폰 없음 - 쿠폰 사용 처리 스킵 - orderId: {}", event.orderId());
        }

    }

    /**
     * 쿠폰 사용 처리
     */
    private void processCouponUsage(Long couponId, Long userId) {
        UserCoupon userCoupon = userCouponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

        // 상태 검증 추가
        if (!userCoupon.isUsable()) {
            log.warn("사용할 수 없는 쿠폰 - couponId: {}, status: {}", couponId, userCoupon.getStatus());
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다. 현재 상태: " + userCoupon.getStatus());
        }

        userCoupon.use(); // AVAILABLE → USED
        log.info("쿠폰 사용 완료 - couponId: {}, userId: {}", couponId, userId);
    }

}
