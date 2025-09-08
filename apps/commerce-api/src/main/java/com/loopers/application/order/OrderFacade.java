package com.loopers.application.order;

import com.loopers.application.dataplatform.event.OrderDataPlatformEvent;
import com.loopers.application.dataplatform.event.PaymentDataPlatformEvent;
import com.loopers.application.order.event.OrderCreatedEvent;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentResult;
import com.loopers.application.product.event.StockDecreasedEvent;
import com.loopers.domain.coupon.*;
import com.loopers.domain.order.*;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.users.User;
import com.loopers.domain.users.UserRepository;
import com.loopers.domain.product.StockChangeResult;
import com.loopers.infrastructure.event.KafkaEventPublisher;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final PaymentFacade paymentFacade;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PointRepository pointRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaEventPublisher kafkaEventPublisher;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order placeOrder(String userId, OrderInfo orderInfo) {

        // 1. 데이터 조회
        // User 조회 (String userId -> Long ID 변환)
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
        Long userInternalId = user.getId();

        List<Long> productIds = orderInfo.items().stream().map(OrderItemInfo::productId).toList();
        List<Product> products =
//                productRepository.findAllById(productIds);
                productRepository.findAllByIdWithLock(productIds);

//        Point userPoint = pointRepository.findByUserId(userId)
        Point userPoint = pointRepository.findByUserIdWithLock(userInternalId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 포인트 정보를 찾을 수 없습니다."));

        // 2. 비즈니스 규칙 검증
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

                    StockChangeResult stockResult = product.decreaseStock(quantity);  // 재고 차감 및 결과 반환

                    // 재고 감소 이벤트 생성
                    StockDecreasedEvent stockEvent = StockDecreasedEvent.forOrder(
                            stockResult.productId(),
                            stockResult.previousStock(),
                            stockResult.currentStock(),
                            stockResult.changedQuantity()
                    );

                    // 동기 집계 처리 (ApplicationEvent)
                    eventPublisher.publishEvent(stockEvent);

                    // 비동기 외부 시스템 연동 (Kafka)
                    kafkaEventPublisher.publish("catalog-events", stockResult.productId().toString(), stockEvent);

                    log.info("재고 감소 이벤트 발행 - productId: {}, 이전재고: {}, 현재재고: {}",
                            stockResult.productId(), stockResult.previousStock(), stockResult.currentStock());


                    return OrderItem.of(product.getId(), quantity, product.getPrice());
                })
                .toList();
        long originalTotalPrice = orderItems.stream().mapToLong(OrderItem::getTotalPrice).sum();

        // 3. 쿠폰 로직 처리
        long discountAmount = 0L;
        if (orderInfo.couponId() != null) {

            // 3-1. 사용자가 보유한 유효한 쿠폰인지 확인
//            UserCoupon userCoupon = userCouponRepository.findByIdAndUserId(orderRequest.couponId(), userId)
            UserCoupon userCoupon = userCouponRepository.findByIdAndUserIdWithLock(orderInfo.couponId(), userInternalId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용할 수 없는 쿠폰입니다."));

            // 3-2. 쿠폰 정책(템플릿) 정보 조회
            Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰 정보를 찾을 수 없습니다."));

            // 3-3. 할인 금액 계산
            DiscountPolicy policy = DiscountPolicyFactory.getPolicy(coupon.getType());
            discountAmount = policy.calculateDiscount(originalTotalPrice, coupon.getDiscountValue());

            // 3-4. 쿠폰 사용 처리 (상태 변경)
//            userCoupon.use();
            if (!userCoupon.isUsable()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
            }

        }

        // 4. 최종 결제 금액 계산
        long finalPrice = originalTotalPrice - discountAmount;

        // 5. 주문 먼저 생성 (PENDING 상태)
        Order newOrder = Order.of(userInternalId, orderItems, discountAmount, orderInfo.couponId(), OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(newOrder);  // PK 생성됨

        // 6. 결제 처리 (orderId와 함께)
        PaymentType paymentType = PaymentType.valueOf(
                orderInfo.paymentType() != null ? orderInfo.paymentType() : "POINT"
        );
        PaymentMethod paymentMethod = createPaymentMethod(orderInfo);


        // 7. 이벤트 발행 (쿠폰 사용 처리)
        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.of(
                savedOrder.getId(),     // orderId
                userId,                 // userId
                orderInfo.couponId(),   // couponId
                finalPrice,             // finalPrice
                paymentType,            // paymentType
                paymentMethod           // paymentMethod
        );

        // 동기 처리: ApplicationEvent
        eventPublisher.publishEvent(orderCreatedEvent);

        // 비동기 처리: Kafka 이벤트 발행 추가
        kafkaEventPublisher.publish(
                "order-events",                    // 토픽명
                savedOrder.getId().toString(),          // PartitionKey = orderId (주문 순서 보장)
                orderCreatedEvent                       // 이벤트 객체
        );

        log.info("주문 생성 완료 및 이벤트 발행 - orderId: {} (ApplicationEvent + Kafka)", savedOrder.getId());

        // 8. 결제 처리
        PaymentResult paymentResult = paymentFacade.processPayment(
                userInternalId,
                savedOrder.getId(),
                finalPrice,
                paymentType,
                paymentMethod
        );

        // 9. PG 결제 요청 결과에 따른 주문 상태 업데이트(실제 결제 성공은 콜백으로 받음)
        if (paymentResult.success()) {
            if (paymentType == PaymentType.POINT) {
                // Point 결제는 즉시 완료 - 같은 트랜잭션에서 처리
                savedOrder.complete();
                log.info("포인트 결제 완료 - 주문 상태 PAID로 변경 - orderId: {}", savedOrder.getId());
                
                // 데이터 플랫폼 이벤트 발행
                publishPointPaymentDataPlatformEvents(savedOrder);
            } else {
                // Card 실제 결제 완료/실패는 PaymentCallbackService에서 처리
                // PG 콜백 대기 상태로 유지 (PROCESSING 상태)
                log.info("카드 결제 요청 완료 - 콜백 대기 중 - orderId: {}, transactionId: {}", savedOrder.getId(), paymentResult.transactionId());
            }
        } else {
            // 실패 케이스
            String message = paymentResult.message();
            // PG 요청 실패(사용자 문제, 잔액 부족 등) → paymentStatus: FAILED
            if (paymentResult.status() == PaymentStatus.FAILED) {
                log.warn("PG 결제 요청 실패 - orderId: {}, reason: {}", savedOrder.getId(), message);
            }
        }

        return savedOrder;
    }

    private PaymentMethod createPaymentMethod(OrderInfo orderInfo) {
        log.info("createPaymentMethod 호출 - orderInfo.paymentMethod(): {}", orderInfo.paymentMethod());
        if (orderInfo.paymentMethod() != null) {
            PaymentMethod paymentMethod = PaymentMethod.of(
                    CardType.valueOf(orderInfo.paymentMethod().cardType()),
                    orderInfo.paymentMethod().cardNo()
            );
            log.info("PaymentMethod 생성 완료 - cardType: {}, cardNo: {}", paymentMethod.getCardType(), paymentMethod.getCardNo());
            return paymentMethod;
        }
        log.warn("PaymentMethod가 null입니다 - orderInfo.paymentMethod()이 null");
        return null;
    }

    /**
     * 포인트 결제 완료 시 데이터 플랫폼 이벤트 발행
     */
    private void publishPointPaymentDataPlatformEvents(Order order) {
        // 주문 데이터 플랫폼 이벤트
        OrderDataPlatformEvent orderEvent = OrderDataPlatformEvent.of(
                order.getId(),
                order.getUserId(),
                order.getFinalPaymentPrice(),
                order.getDiscountAmount(),
                order.getStatus().name()
        );
        eventPublisher.publishEvent(orderEvent);
        log.info("포인트 결제 - 주문 데이터 플랫폼 이벤트 발행 - orderId: {}", order.getId());

        // 결제 데이터 플랫폼 이벤트
        PaymentDataPlatformEvent paymentEvent = PaymentDataPlatformEvent.of(
                order.getId(),
                order.getUserId(),
                "POINT",
                order.getFinalPaymentPrice(),
                "SUCCESS",
                "point-payment-" + order.getId() // 포인트 결제는 별도 트랜잭션키 생성
        );
        eventPublisher.publishEvent(paymentEvent);
        log.info("포인트 결제 - 결제 데이터 플랫폼 이벤트 발행 - orderId: {}", order.getId());
    }

}
