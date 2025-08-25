package com.loopers.application.payment;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.coupon.CouponApplicationService;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.points.PointApplicationService;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.coupon.UserCouponStatus;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.*;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.interfaces.api.payment.PgCallbackRequest;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentUsecaseIntegrationTest {

    @Autowired private PaymentFacade paymentFacade;
    @Autowired private UserApplicationService userAppService;
    @Autowired private BrandApplicationService brandAppService;
    @Autowired private ProductFacade productFacade;
    @Autowired private PointApplicationService pointAppService;
    @Autowired private CouponApplicationService couponAppService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PointRepository pointRepository;
    @Autowired private UserCouponRepository userCouponRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    @MockitoBean private PgClient pgClient;

    private UserInfo testUser;
    private ProductResponse testProduct;
    private UserCoupon testCoupon;

    @BeforeEach
    void setUp() {
        testUser = userAppService.saveUser("testUser", "M", "2000-01-01", "test@example.com");
        pointAppService.chargePoint(testUser.userId(), 100000L);

        BrandInfo brand = brandAppService.create("테스트브랜드", "설명", true);
        testProduct = productFacade.create(brand.id(), "테스트상품", "", 10000, 20, 10, ProductStatus.ACTIVE);

        // 쿠폰 생성 및 발급
        CouponInfo couponTemplate = couponAppService.createCoupon("1000원 할인쿠폰", "테스트 쿠폰", CouponType.FIXED, 1000, 100, 
                ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(30));
        couponAppService.issueCouponToUser(testUser.userId(), couponTemplate.id());
        testCoupon = userCouponRepository.findByUserIdAndStatus(testUser.id(), UserCouponStatus.AVAILABLE, PageRequest.of(0, 10))
                .getContent().get(0);
    }

    /**
     * 각 테스트에서 독립적인 Order 생성을 위한 헬퍼 메서드
     */
    private Order createTestOrder() {
        java.util.List<OrderItem> orderItems = new java.util.ArrayList<>();
        orderItems.add(OrderItem.of(testProduct.productId(), 1, 10000L));
        Order order = Order.of(testUser.id(), orderItems, 0L, OrderStatus.PENDING);
        return orderRepository.save(order);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 결제 통합 테스트")
    @Nested
    class PointPaymentIntegration {

        @DisplayName("포인트 결제 시 즉시 성공하고 포인트가 차감된다.")
        @Test
        void processPointPayment_deductsPointsImmediately() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            Long userId = testUser.id();
            Long orderId = testOrder.getId();
            long amount = 5000L;
            long initialPoints = pointRepository.findByUserId(testUser.id()).get().getPoint();

            // act
            PaymentResult result = paymentFacade.processPayment(userId, orderId, amount, PaymentType.POINT, null);

            // assert
            assertThat(result.success()).isTrue();
            
            Point finalPoint = pointRepository.findByUserId(testUser.id()).get();
            assertThat(finalPoint.getPoint()).isEqualTo(initialPoints - amount);
        }

        @DisplayName("포인트 부족 시 결제가 실패한다.")
        @Test
        void processPointPayment_failsWhenInsufficientPoints() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            Long userId = testUser.id();
            Long orderId = testOrder.getId();
            long amount = 200000L; // 보유 포인트보다 많은 금액

            // act & assert
            assertThatThrownBy(() -> paymentFacade.processPayment(userId, orderId, amount, PaymentType.POINT, null))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("카드 결제 통합 테스트")
    @Nested
    class CardPaymentIntegration {

        @DisplayName("카드 결제 성공 시 Payment가 저장되고 transactionKey가 업데이트된다.")
        @Test
        void processCardPayment_savesPaymentAndUpdatesTransactionKey_whenPgSucceeds() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            Long userId = testUser.id();
            Long orderId = testOrder.getId();
            long amount = 10000L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");

            PgPaymentResponse pgResponse = PgPaymentResponse.success("20250822:TR:success123", "PENDING");
            when(pgClient.requestPayment(anyString(), any())).thenReturn(pgResponse);

            // act
            PaymentResult result = paymentFacade.processPayment(userId, orderId, amount, PaymentType.CARD, paymentMethod);

            // assert
            assertThat(result.success()).isTrue();

            Optional<Payment> savedPayment = paymentRepository.findByOrderId(orderId);
            assertThat(savedPayment).isPresent();
            assertThat(savedPayment.get().getTransactionKey()).isEqualTo("20250822:TR:success123");
            assertThat(savedPayment.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @DisplayName("PG 장애 시에도 Fallback이 동작하여 처리된다.")
        @Test
        void processCardPayment_handlesPgFailureWithFallback() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            Long userId = testUser.id();
            Long orderId = testOrder.getId();
            long amount = 10000L;
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");

            when(pgClient.requestPayment(anyString(), any())).thenThrow(new RuntimeException("PG 시스템 장애"));

            // act
            PaymentResult result = paymentFacade.processPayment(userId, orderId, amount, PaymentType.CARD, paymentMethod);

            // assert
            assertThat(result.success()).isFalse();

            Optional<Payment> savedPayment = paymentRepository.findByOrderId(orderId);
            assertThat(savedPayment).isPresent();
            assertThat(savedPayment.get().getTransactionKey()).startsWith("FALLBACK_");
        }
    }

    @DisplayName("결제 콜백 처리 통합 테스트")
    @Nested
    class PaymentCallbackIntegration {

        @DisplayName("성공 콜백 수신 시 Payment와 Order 상태가 업데이트된다.")
        @Test
        void handlePaymentCallback_updatesPaymentAndOrderStatus_whenSuccess() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            String transactionKey = "20250822:TR:callback123";

            Payment payment = Payment.of(testOrder.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 10000L);
            payment.updateTransactionKey(transactionKey);
            Payment savedPayment = paymentRepository.save(payment);

            PgCallbackRequest callbackRequest = new PgCallbackRequest(
                transactionKey, String.valueOf(testOrder.getId()), "SUCCESS", 10000L, "결제 성공", "SAMSUNG", "2025-08-22T10:00:00Z"
            );

            // act
            paymentFacade.handlePaymentCallback(callbackRequest);

            // assert
            Optional<Payment> updatedPayment = paymentRepository.findByTransactionKey(transactionKey);
            assertThat(updatedPayment).isPresent();
            assertThat(updatedPayment.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            Optional<Order> updatedOrder = orderRepository.findByIdWithItems(testOrder.getId());
            assertThat(updatedOrder).isPresent();
            assertThat(updatedOrder.get().getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @DisplayName("실패 콜백 수신 시 Payment는 실패로, Order는 취소로 업데이트된다.")
        @Test
        void handlePaymentCallback_updatesPaymentAndOrderStatus_whenFailed() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            String transactionKey = "20250822:TR:failed456";

            Payment payment = Payment.of(testOrder.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 5000L);
            payment.updateTransactionKey(transactionKey);
            Payment savedPayment = paymentRepository.save(payment);

            PgCallbackRequest callbackRequest = new PgCallbackRequest(
                transactionKey, String.valueOf(testOrder.getId()), "FAILED", 5000L, "한도초과", "SAMSUNG", "2025-08-22T10:00:00Z"
            );

            // act
            paymentFacade.handlePaymentCallback(callbackRequest);

            // assert
            Optional<Payment> updatedPayment = paymentRepository.findByTransactionKey(transactionKey);
            assertThat(updatedPayment).isPresent();
            assertThat(updatedPayment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);

            Optional<Order> updatedOrder = orderRepository.findByIdWithItems(testOrder.getId());
            assertThat(updatedOrder).isPresent();
            assertThat(updatedOrder.get().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @DisplayName("결제 상태 동기화 통합 테스트")
    @Nested
    class PaymentStatusSyncIntegration {

        @DisplayName("PG에서 상태를 조회하여 로컬 상태가 동기화된다.")
        @Test
        void syncPaymentStatus_updatesLocalStatusFromPgResponse() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            String transactionKey = "20250822:TR:sync123";

            Payment payment = Payment.of(testOrder.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 15000L);
            payment.updateTransactionKey(transactionKey);
            Payment savedPayment = paymentRepository.save(payment);

            PgPaymentResponse pgResponse = PgPaymentResponse.success(transactionKey, "SUCCESS");
            when(pgClient.getPayment(anyString(), anyString())).thenReturn(pgResponse);

            // act
            paymentFacade.syncPaymentStatus(transactionKey, testUser.id().toString());

            // assert
            Optional<Payment> updatedPayment = paymentRepository.findByTransactionKey(transactionKey);
            assertThat(updatedPayment).isPresent();
            assertThat(updatedPayment.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            Optional<Order> updatedOrder = orderRepository.findByIdWithItems(testOrder.getId());
            assertThat(updatedOrder).isPresent();
            assertThat(updatedOrder.get().getStatus()).isEqualTo(OrderStatus.PAID);
        }
    }

    @DisplayName("결제 상태 확인 통합 테스트")
    @Nested
    class PaymentStatusCheckIntegration {

        @DisplayName("transactionKey로 결제 상태를 조회할 수 있다.")
        @Test
        void checkPaymentStatus_returnsPaymentInfo() {
            // arrange
            Order testOrder = createTestOrder(); // 독립적인 Order 생성
            String transactionKey = "20250822:TR:check789";

            Payment payment = Payment.of(testOrder.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 20000L);
            payment.updateTransactionKey(transactionKey);
            payment.markAsSuccess(transactionKey);
            paymentRepository.save(payment);

            // act
            PaymentStatusInfo result = paymentFacade.checkPaymentStatus(transactionKey, testUser.id().toString());

            // assert
            assertThat(result).isNotNull();
            assertThat(result.transactionKey()).isEqualTo(transactionKey);
            assertThat(result.ourStatus()).isEqualTo("SUCCESS");
        }
    }

    @DisplayName("결제 실패 시 복구 로직 테스트")
    @Nested
    class PaymentFailureRecoveryTest {

        @DisplayName("PG 콜백으로 결제 실패 시 재고와 쿠폰이 복구된다.")
        @Test
        void restoreResourcesWhenPaymentFails_throughCallback() {
            // arrange
            Order testOrder = createTestOrderWithCoupon();
            int orderQuantity = 2;
            int initialStock = productRepository.productInfo(testProduct.productId()).get().getStock();
            
            // 재고를 수동으로 차감 (주문 과정 시뮬레이션)
            Product product = productRepository.productInfo(testProduct.productId()).get();
            product.decreaseStock(orderQuantity);
            productRepository.save(product);
            
            // 쿠폰을 사용 상태로 변경 (주문 과정 시뮬레이션)
            testCoupon.use();
            userCouponRepository.save(testCoupon);
            
            // Payment 생성 및 저장
            Payment payment = Payment.of(testOrder.getId(), PaymentMethod.of(CardType.SAMSUNG, "1234-1234-1234-1234"), 15000L);
            String transactionKey = "TEST:TR:failure123";
            payment.updateTransactionKey(transactionKey);
            paymentRepository.save(payment);

            // 사전 상태 확인
            Product productBeforeCallback = productRepository.productInfo(testProduct.productId()).get();
            UserCoupon couponBeforeCallback = userCouponRepository.findById(testCoupon.getId()).get();
            
            assertThat(productBeforeCallback.getStock()).isEqualTo(initialStock - orderQuantity); // 재고 차감됨
            assertThat(couponBeforeCallback.getStatus()).isEqualTo(UserCouponStatus.USED); // 쿠폰 사용됨

            // act: PG 결제 실패 콜백 처리
            PgCallbackRequest failureCallback = new PgCallbackRequest(
                    transactionKey, 
                    testOrder.getId().toString(), 
                    "FAILED", 
                    15000L, 
                    "한도 초과", 
                    "SAMSUNG", 
                    "2025-08-23T10:30:00Z"
            );
            paymentFacade.handlePaymentCallback(failureCallback);

            // assert: 복구 상태 확인
            // 1. 주문이 취소됨
            Order orderAfterCallback = orderRepository.findByIdWithItems(testOrder.getId()).get();
            assertThat(orderAfterCallback.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            
            // 2. 결제가 실패 상태로 변경됨
            Payment paymentAfterCallback = paymentRepository.findByTransactionKey(transactionKey).get();
            assertThat(paymentAfterCallback.getStatus()).isEqualTo(PaymentStatus.FAILED);
            
            // 3. 재고가 복구됨
            Product productAfterCallback = productRepository.productInfo(testProduct.productId()).get();
            assertThat(productAfterCallback.getStock()).isEqualTo(initialStock); // 원래 재고로 복구
            
            // 4. 쿠폰이 복구됨
            UserCoupon couponAfterCallback = userCouponRepository.findById(testCoupon.getId()).get();
            assertThat(couponAfterCallback.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE); // 사용 가능으로 복구
            assertThat(couponAfterCallback.getUsedAt()).isNull(); // 사용 시간 초기화
        }

        private Order createTestOrderWithCoupon() {
            java.util.List<OrderItem> orderItems = new java.util.ArrayList<>();
            orderItems.add(OrderItem.of(testProduct.productId(), 2, 10000L));
            Order order = Order.of(testUser.id(), orderItems, 1000L, testCoupon.getId(), OrderStatus.PENDING);
            return orderRepository.save(order);
        }
    }
}