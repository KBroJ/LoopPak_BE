package com.loopers.application.payment.event;

import com.loopers.application.dataplatform.event.OrderDataPlatformEvent;
import com.loopers.application.dataplatform.event.PaymentDataPlatformEvent;
import com.loopers.application.payment.PaymentRecoveryService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
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
    private final ApplicationEventPublisher eventPublisher;
    
    @EventListener
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("결제 성공 이벤트 처리 시작 - 주문 완료 - orderId: {}", event.orderId());

        processOrderCompletion(event.orderId());
        
        // 데이터 플랫폼 이벤트 발행 (실제 이벤트 데이터 사용)
        publishDataPlatformEventsForSuccess(event);

        log.info("주문 완료 처리 완료 - orderId: {}", event.orderId());
    }

    @EventListener
    @Transactional
    public void handlePaymentFailure(PaymentFailureEvent event) {
        log.info("결제 실패 이벤트 처리 시작 - 주문 취소 - orderId: {}", event.orderId());

        processOrderCancellation(event.orderId(), event.failureReason());
        
        // 데이터 플랫폼 이벤트 발행 (실제 이벤트 데이터 사용)
        publishDataPlatformEventsForFailure(event);

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

    private void publishDataPlatformEventsForSuccess(PaymentSuccessEvent event) {
        Order order = orderRepository.findByIdWithItems(event.orderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        // 주문 데이터 플랫폼 이벤트
        OrderDataPlatformEvent orderEvent = OrderDataPlatformEvent.of(
                order.getId(),
                order.getUserId(),
                order.getFinalPaymentPrice(),
                order.getDiscountAmount(),
                order.getStatus().name()
        );
        eventPublisher.publishEvent(orderEvent);
        log.info("주문 데이터 플랫폼 이벤트 발행 - orderId: {}", order.getId());

        // 결제 데이터 플랫폼 이벤트
        PaymentDataPlatformEvent paymentEvent = PaymentDataPlatformEvent.of(
                event.orderId(),
                event.userId(),
                event.paymentType().name(),
                event.amount(),
                "SUCCESS",
                event.transactionKey()
        );
        eventPublisher.publishEvent(paymentEvent);
        log.info("결제 데이터 플랫폼 이벤트 발행 - orderId: {}, paymentType: {}", event.orderId(), event.paymentType());
    }

    private void publishDataPlatformEventsForFailure(PaymentFailureEvent event) {
        Order order = orderRepository.findByIdWithItems(event.orderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        // 주문 데이터 플랫폼 이벤트
        OrderDataPlatformEvent orderEvent = OrderDataPlatformEvent.of(
                order.getId(),
                order.getUserId(),
                order.getFinalPaymentPrice(),
                order.getDiscountAmount(),
                order.getStatus().name()
        );
        eventPublisher.publishEvent(orderEvent);
        log.info("주문 데이터 플랫폼 이벤트 발행 - orderId: {}", order.getId());

        // 결제 데이터 플랫폼 이벤트
        PaymentDataPlatformEvent paymentEvent = PaymentDataPlatformEvent.of(
                event.orderId(),
                event.userId(),
                event.paymentType().name(),
                event.amount(),
                "FAILED",
                event.transactionKey()
        );
        eventPublisher.publishEvent(paymentEvent);
        log.info("결제 데이터 플랫폼 이벤트 발행 - orderId: {}, paymentType: {}", event.orderId(), event.paymentType());
    }

}
