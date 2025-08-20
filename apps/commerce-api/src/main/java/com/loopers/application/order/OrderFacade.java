package com.loopers.application.order;

import com.loopers.domain.coupon.*;
import com.loopers.domain.order.*;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PointRepository pointRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public Order placeOrder(Long userId, OrderInfo orderInfo) {

        // === 1. 데이터 조회 ===
        List<Long> productIds = orderInfo.items().stream().map(OrderItemInfo::productId).toList();
        List<Product> products =
//                productRepository.findAllById(productIds);
                productRepository.findAllByIdWithLock(productIds);

//        Point userPoint = pointRepository.findByUserId(userId)
        Point userPoint = pointRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 포인트 정보를 찾을 수 없습니다."));

        // === 2. 비즈니스 규칙 검증 ===
        // 요청된 상품이 존재하는지 확인
        if (productIds.size() != products.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품 정보를 찾을 수 없습니다.");
        }

        Map<Long, Integer> quantityMap = orderInfo.items().stream()
                .collect(Collectors.toMap(OrderItemInfo::productId, OrderItemInfo::quantity));

        // 주문 아이템 생성 및 원가 계산
        List<OrderItem> orderItems = products.stream()
                .map(product -> {
                    int quantity = quantityMap.get(product.getId());
                    product.decreaseStock(quantity);    // Product Entity가 스스로 재고를 차감하고, 불가능하면 예외를 던짐
                    return OrderItem.of(product.getId(), quantity, product.getPrice());
                })
                .toList();
        long originalTotalPrice = orderItems.stream().mapToLong(OrderItem::getTotalPrice).sum();

        // === 3. 쿠폰 로직 처리 ===
        long discountAmount = 0L;
        if (orderInfo.couponId() != null) {

            // 3-1. 사용자가 보유한 유효한 쿠폰인지 확인
//            UserCoupon userCoupon = userCouponRepository.findByIdAndUserId(orderRequest.couponId(), userId)
            UserCoupon userCoupon = userCouponRepository.findByIdAndUserIdWithLock(orderInfo.couponId(), userId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용할 수 없는 쿠폰입니다."));

            // 3-2. 쿠폰 정책(템플릿) 정보 조회
            Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 정보를 찾을 수 없습니다."));

            // 3-3. 할인 금액 계산
            DiscountPolicy policy = DiscountPolicyFactory.getPolicy(coupon.getType());
            discountAmount = policy.calculateDiscount(originalTotalPrice, coupon.getDiscountValue());

            // 3-4. 쿠폰 사용 처리 (상태 변경)
            userCoupon.use();

        }

        // === 4. 최종 결제 금액 계산 ===
        long finalPrice = originalTotalPrice - discountAmount;

        // === 5. 결제 방식 처리 ===
        PaymentType paymentType = PaymentType.valueOf(
            orderInfo.paymentType() != null ? orderInfo.paymentType() : "POINT"
        );
        PaymentMethod paymentMethod = null;

        if (paymentType == PaymentType.CARD && orderInfo.cardInfo() != null) {
            paymentMethod = PaymentMethod.of(
                    CardType.valueOf(orderInfo.cardInfo().cardType()),
                    orderInfo.cardInfo().cardNo()
            );
        }

        // 포인트 결제는 즉시 처리
        if (paymentType == PaymentType.POINT) {
            userPoint.use(finalPrice);
        }

        // 결제 방법에 따른 주문 상태 결정
        OrderStatus orderStatus = (paymentType == PaymentType.POINT) ? OrderStatus.PAID : OrderStatus.PENDING;

        // === 6. 주문 생성 및 저장 ===
        Order newOrder = Order.of(userId, orderItems, discountAmount, orderStatus);
        return orderRepository.save(newOrder);
    }

}
