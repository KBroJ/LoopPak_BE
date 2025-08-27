package com.loopers.application.order;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.coupon.UserCouponStatus;
import com.loopers.domain.order.OrderRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * 주문 생성 후 쿠폰 사용 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 주문이 확실히 커밋된 후
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 처리 시작 - 쿠폰 사용 처리 - orderId: {}", event.orderId());

        try {
            // === 오직 쿠폰 사용 처리만 ===
            if (event.couponId() != null) {
                processCouponUsage(event.couponId(), event.userId());
                log.info("쿠폰 사용 처리 완료 - orderId: {}, couponId: {}",
                        event.orderId(), event.couponId());
            } else {
                log.info("쿠폰 없음 - 쿠폰 사용 처리 스킵 - orderId: {}", event.orderId());
            }

        } catch (Exception e) {
            log.error("쿠폰 사용 처리 실패 - orderId: {}, couponId: {}, error: {}",
                    event.orderId(), event.couponId(), e.getMessage(), e);
            // TODO: 쿠폰 사용 실패 시 처리 (재시도, 알림 등)
        }
    }

    /**
     * 쿠폰 사용 처리 (별도 트랜잭션)
     */
    @Transactional
    private void processCouponUsage(Long couponId, Long userId) {
        UserCoupon userCoupon = userCouponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

        // 상태 검증 추가
        if (userCoupon.getStatus() != UserCouponStatus.RESERVED) {
            log.warn("쿠폰이 RESERVED 상태가 아님 - couponId: {}, status: {}",
                    couponId, userCoupon.getStatus());
            return; // 또는 적절한 예외 처리
        }

        userCoupon.confirmUsage(); // RESERVED → USED
        log.info("쿠폰 사용 확정 완료 - couponId: {}, userId: {}", couponId, userId);
    }

}
