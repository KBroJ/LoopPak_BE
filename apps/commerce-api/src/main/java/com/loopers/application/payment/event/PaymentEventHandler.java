package com.loopers.application.payment.event;

import com.loopers.application.payment.PaymentRecoveryService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 관련 이벤트 처리 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final OrderRepository orderRepository;
    private final PaymentRecoveryService paymentRecoveryService;
    
    @EventListener
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("결제 성공 이벤트 처리 시작 - 주문 완료 - orderId: {}", event.orderId());

        processOrderCompletion(event.orderId());

        log.info("주문 완료 처리 완료 - orderId: {}", event.orderId());
    }

    @EventListener
    @Transactional
    public void handlePaymentFailure(PaymentFailureEvent event) {
        log.info("결제 실패 이벤트 처리 시작 - 주문 취소 - orderId: {}", event.orderId());

        processOrderCancellation(event.orderId(), event.failureReason());

        log.info("주문 취소 및 리소스 복구 완료 - orderId: {}", event.orderId());
    }

    private void processOrderCompletion(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        order.complete();
        orderRepository.save(order);
    }

    private void processOrderCancellation(Long orderId, String reason) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        order.cancel("결제 실패: " + reason);
        orderRepository.save(order);

        // 리소스 복구
        paymentRecoveryService.restoreOrderResources(order);
    }

}
