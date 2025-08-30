package com.loopers.application.dataplatform.event;

import com.loopers.application.dataplatform.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 데이터 플랫폼 전송 이벤트 처리 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final DataPlatformClient dataPlatformClient;

    @Async
    @EventListener
    public void handleOrderDataPlatform(OrderDataPlatformEvent event) {
        log.info("주문 데이터 플랫폼 전송 이벤트 처리 시작 - orderId: {}", event.orderId());

        try {
            dataPlatformClient.sendOrderData(
                    event.orderId(),
                    event.userId(),
                    event.totalAmount(),
                    event.discountAmount(),
                    event.orderStatus()
            );
            
            log.info("주문 데이터 플랫폼 전송 완료 - orderId: {}", event.orderId());
        } catch (Exception e) {
            // 외부 시스템 실패가 메인 비즈니스에 영향을 주면 안되므로 로그만 남김
            log.error("주문 데이터 플랫폼 전송 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void handlePaymentDataPlatform(PaymentDataPlatformEvent event) {
        log.info("결제 데이터 플랫폼 전송 이벤트 처리 시작 - orderId: {}", event.orderId());

        try {
            dataPlatformClient.sendPaymentData(
                    event.orderId(),
                    event.userId(),
                    event.paymentType(),
                    event.amount(),
                    event.paymentStatus(),
                    event.transactionKey()
            );
            
            log.info("결제 데이터 플랫폼 전송 완료 - orderId: {}", event.orderId());
        } catch (Exception e) {
            // 외부 시스템 실패가 메인 비즈니스에 영향을 주면 안되므로 로그만 남김
            log.error("결제 데이터 플랫폼 전송 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
        }
    }
}