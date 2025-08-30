package com.loopers.application.dataplatform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 데이터 플랫폼 클라이언트
 * 실제 구현에서는 외부 API 호출을 수행
 */
@Slf4j
@Component
public class DataPlatformClient {

    public void sendOrderData(Long orderId, Long userId, Long totalAmount, Long discountAmount, String orderStatus) {
        log.info("데이터 플랫폼으로 주문 데이터 전송 - orderId: {}, userId: {}, totalAmount: {}, discountAmount: {}, orderStatus: {}", 
                orderId, userId, totalAmount, discountAmount, orderStatus);
        
        // Mock: 외부 API 호출 시뮬레이션
        simulateExternalApiCall("ORDER_DATA");
        
        log.info("주문 데이터 전송 완료 - orderId: {}", orderId);
    }

    public void sendPaymentData(Long orderId, Long userId, String paymentType, Long amount, String paymentStatus, String transactionKey) {
        log.info("데이터 플랫폼으로 결제 데이터 전송 - orderId: {}, userId: {}, paymentType: {}, amount: {}, paymentStatus: {}, transactionKey: {}", 
                orderId, userId, paymentType, amount, paymentStatus, transactionKey);
        
        // Mock: 외부 API 호출 시뮬레이션
        simulateExternalApiCall("PAYMENT_DATA");
        
        log.info("결제 데이터 전송 완료 - orderId: {}", orderId);
    }

    private void simulateExternalApiCall(String dataType) {
        try {
            // Mock: API 응답 시간 시뮬레이션 (100ms)
            Thread.sleep(100);
            log.debug("외부 API 호출 완료 - dataType: {}", dataType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("외부 API 호출 중 인터럽트 발생 - dataType: {}", dataType);
        }
    }
}