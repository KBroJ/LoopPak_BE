package com.loopers.application.order.event;

import com.loopers.application.payment.event.PaymentCompletedEvent;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.PaymentStatus;
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

    private final OrderRepository orderRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 주문 생성 후 쿠폰 사용 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) // 주문 트랜잭션 커밋 전
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 처리 시작 - 쿠폰 사용 처리 - orderId: {}", event.orderId());

        // === 오직 쿠폰 사용 처리만 ===
        if (event.couponId() != null) {
            processCouponUsage(event.couponId(), event.userId()); // 예외 발생 시 트랜잭션 롤백!
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


    /**
     * 결제 성공 시 주문 완료 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 성공 이벤트 처리 시작 - 주문 완료 - orderId: {}", event.orderId());

        Order order = orderRepository.findByIdWithItems(event.orderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if (event.paymentStatus() == PaymentStatus.SUCCESS) {
            order.complete();  // PAID 상태로
            log.info("주문 완료 처리 완료 - orderId: {}", event.orderId());
        }
    }

    /**
     * 결제 실패 시 주문 취소 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 처리 시작 - 주문 취소 - orderId: {}", event.orderId());

        Order order = orderRepository.findByIdWithItems(event.orderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        order.cancel("결제 실패: " + event.failureReason());
        log.info("주문 취소 처리 완료 - orderId: {}", event.orderId());
    }

}
