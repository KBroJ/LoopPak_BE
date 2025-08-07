package com.loopers.application.order;

import com.loopers.domain.coupon.*;
import com.loopers.domain.order.*;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PointRepository pointRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public Order placeOrder(Long userId, OrderRequest orderRequest) {

        // === 1. 데이터 조회 ===
        List<Long> productIds = orderRequest.items().stream().map(OrderItemRequest::productId).toList();
        List<Product> products = productRepository.findAllById(productIds);
        Point userPoint = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 포인트 정보를 찾을 수 없습니다."));

        // === 2. 비즈니스 규칙 검증 ===
        // 요청된 상품이 존재하는지 확인
        if (productIds.size() != products.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품 정보를 찾을 수 없습니다.");
        }

        Map<Long, Integer> quantityMap = orderRequest.items().stream()
                .collect(Collectors.toMap(OrderItemRequest::productId, OrderItemRequest::quantity));

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
        if (orderRequest.couponId() != null) {
            // 3-1. 사용자가 보유한 유효한 쿠폰인지 확인
            UserCoupon userCoupon = userCouponRepository.findByIdAndUserId(orderRequest.couponId(), userId)
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

        // === 4. 최종 결제 금액 계산 및 포인트 사용 ===
        long finalPrice = originalTotalPrice - discountAmount;
        userPoint.use(finalPrice);

        // === 5. 주문 생성 및 저장 ===
        Order newOrder = Order.of(userId, orderItems, discountAmount);
        return orderRepository.save(newOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        // DTO 변환 로직은 여기에 직접 두거나, 별도 Mapper 클래스로 분리할 수 있습니다.
        return orderPage.map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문 정보를 찾을 수 없습니다."));

        List<Long> productIds = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .toList();

        // Product 정보를 Map 형태로 한번에 조회
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        return OrderDetailResponse.of(order, productMap);
    }

}
