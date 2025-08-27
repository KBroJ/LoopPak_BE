package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.interfaces.api.payment.PgCallbackRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 콜백 처리 전담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentRecoveryService paymentRecoveryService;

    @Transactional
    public void handlePaymentCallback(PgCallbackRequest callbackRequest) {
        log.info("결제 콜백 처리 시작 - transactionKey: {}", callbackRequest.transactionKey());

        // 1. 유효성 검증
        validateCallbackRequest(callbackRequest);

        // 2. Payment 조회
        Payment payment = findPaymentByTransactionKey(callbackRequest.transactionKey());

        // 3. 멱등성 검증 (이미 처리 완료된 경우 중복 처리 방지)
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("이미 처리된 결제 콜백 - transactionKey: {}, currentStatus: {}",
                    callbackRequest.transactionKey(), payment.getStatus());
            return; // 중복 처리 방지
        }

        // 4. 결제 상태 업데이트
        updatePaymentStatus(payment, callbackRequest);

        // 5. Order 상태 연동
        updateRelatedOrderStatus(payment, callbackRequest.isSuccess());

        log.info("결제 콜백 처리 완료 - transactionKey: {}, success: {}",
                callbackRequest.transactionKey(), callbackRequest.isSuccess());
    }

    private void validateCallbackRequest(PgCallbackRequest callbackRequest) {
        if (callbackRequest.transactionKey() == null || callbackRequest.transactionKey().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "transactionKey가 없습니다.");
        }
    }

    private Payment findPaymentByTransactionKey(String transactionKey) {
        return paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다: " + transactionKey));
    }

    private void updatePaymentStatus(Payment payment, PgCallbackRequest callbackRequest) {
        if (callbackRequest.isSuccess()) {
            payment.markAsSuccess(callbackRequest.transactionKey());
            log.info("결제 성공 처리 - transactionKey: {}", callbackRequest.transactionKey());
        } else {
            payment.markAsFailed();
            log.warn("결제 실패 처리 - transactionKey: {}, reason: {}",
                    callbackRequest.transactionKey(), callbackRequest.message());
        }
    }

    public void updateRelatedOrderStatus(Payment payment, boolean paymentSuccess) {
        try {
            // Payment의 orderId로 Order 조회
            Order order = orderRepository.findByIdWithItems(payment.getOrderId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                            "주문 정보를 찾을 수 없습니다: " + payment.getOrderId()));

            if (paymentSuccess) {
                // 결제 성공 시 주문 완료
                log.info("주문 완료 처리 - orderId: {}, paymentId: {}", payment.getOrderId(), payment.getId());
                order.complete();
                orderRepository.save(order);
            } else {
                // 결제 실패 시 주문 취소 및 리소스 복구
                log.warn("결제 실패로 인한 주문 취소 및 복구 처리 - orderId: {}, paymentId: {}",
                        payment.getOrderId(), payment.getId());
                order.cancel("결제 실패");
                orderRepository.save(order);

                // 자원 복구 서비스에 위임
                paymentRecoveryService.restoreOrderResources(order);
            }

        } catch (Exception e) {
            log.error("주문 상태 업데이트 실패 - orderId: {}, error: {}", payment.getOrderId(), e.getMessage(), e);
        }
    }

}
