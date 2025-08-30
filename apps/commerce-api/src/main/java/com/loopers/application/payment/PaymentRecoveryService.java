package com.loopers.application.payment;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 결제 실패 시 자원 복구 전담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRecoveryService {

    private final ProductRepository productRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 주문 실패 시 재고 및 쿠폰 복구
     */
    public void restoreOrderResources(Order order) {
        try {
            log.info("주문 리소스 복구 시작 - orderId: {}", order.getId());

            // 1. 재고 복구
            restoreProductStock(order);

            // 2. 쿠폰 복구 (사용된 쿠폰이 있는 경우)
            if (order.getCouponId() != null) {
                restoreCoupon(order.getCouponId());
            }

            log.info("주문 리소스 복구 완료 - orderId: {}", order.getId());

        } catch (Exception e) {
            log.error("주문 리소스 복구 실패 - orderId: {}, error: {}", order.getId(), e.getMessage(), e);
            // 복구 실패는 로그만 남기고 예외를 다시 던지지 않음 (결제 처리는 계속 진행)
        }
    }

    /**
     * 상품 재고 복구
     */
    private void restoreProductStock(Order order) {
        log.info("재고 복구 시작 - orderId: {}, 상품 수: {}", order.getId(), order.getOrderItems().size());

        order.getOrderItems().forEach(orderItem -> {
            try {
                Product product = productRepository.productInfo(orderItem.getProductId())
                        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                                "상품을 찾을 수 없습니다: " + orderItem.getProductId()));

                // 재고 복구 (주문했던 수량만큼 다시 증가)
                product.increaseStock(orderItem.getQuantity());
                productRepository.save(product);

                log.info("재고 복구 완료 - productId: {}, quantity: {}, 복구 후 재고: {}",
                        orderItem.getProductId(), orderItem.getQuantity(), product.getStock());

            } catch (Exception e) {
                log.error("재고 복구 실패 - productId: {}, quantity: {}, error: {}",
                        orderItem.getProductId(), orderItem.getQuantity(), e.getMessage(), e);
            }
        });
    }

    /**
     * 쿠폰 복구
     */
    private void restoreCoupon(Long couponId) {
        try {
            UserCoupon userCoupon = userCouponRepository.findById(couponId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다: " + couponId));

            // 쿠폰 복구 (USED → AVAILABLE)
            userCoupon.restore();
            userCouponRepository.save(userCoupon);

            log.info("쿠폰 복구 완료 - couponId: {}, 복구 후 상태: {}", couponId, userCoupon.getStatus());

        } catch (Exception e) {
            log.error("쿠폰 복구 실패 - couponId: {}, error: {}", couponId, e.getMessage(), e);
        }
    }

}
