package com.loopers.application.dataplatform;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.dataplatform.event.OrderDataPlatformEvent;
import com.loopers.application.dataplatform.event.PaymentDataPlatformEvent;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.CardInfo;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.application.payment.PaymentCallbackService;
import com.loopers.application.points.PointApplicationService;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.interfaces.api.payment.PgCallbackRequest;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 데이터 플랫폼 이벤트 발행 통합 테스트
 */
@SpringBootTest
@RecordApplicationEvents
class DataPlatformEventIntegrationTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private UserApplicationService userAppService;
    @Autowired private PointApplicationService pointAppService;
    @Autowired private BrandApplicationService brandAppService;
    @Autowired private ProductFacade productFacade;
    @Autowired private PaymentCallbackService paymentCallbackService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ApplicationEvents events;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    private UserInfo testUser;
    private ProductResponse testProduct;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        
        // 테스트 데이터 준비
        testUser = userAppService.saveUser("testUser", "M", "2000-01-01", "test@example.com");
        pointAppService.chargePoint(testUser.userId(), 100000L);

        BrandInfo brand = brandAppService.create("테스트브랜드", "설명", true);
        testProduct = productFacade.create(brand.id(), "테스트상품", "", 10000, 20, 10, ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("포인트 결제 완료 시 데이터 플랫폼 이벤트가 발행된다")
    void shouldPublishDataPlatformEventsOnPointPaymentSuccess() throws InterruptedException {
        // Given: 포인트 결제 주문
        OrderInfo orderInfo = new OrderInfo(
                List.of(new OrderItemInfo(testProduct.productId(), 1)),
                null, // 쿠폰 없음
                "POINT",
                null // 포인트 결제는 결제 수단 정보 불필요
        );

        // When: 포인트 결제 (즉시 완료됨)
        Order order = orderFacade.placeOrder(testUser.id(), orderInfo);

        // 비동기 이벤트 처리를 위한 대기
        Thread.sleep(500);

        // Then: 데이터 플랫폼 이벤트들이 발행되었는지 확인
        // 포인트 결제는 즉시 완료되므로 SUCCESS 이벤트가 발행되어야 함
        assertThat(events.stream(OrderDataPlatformEvent.class))
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(order.getId());
                    assertThat(event.userId()).isEqualTo(testUser.id());
                    assertThat(event.orderStatus()).isEqualTo(OrderStatus.PAID.name());
                });

        assertThat(events.stream(PaymentDataPlatformEvent.class))
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(order.getId());
                    assertThat(event.userId()).isEqualTo(testUser.id());
                    assertThat(event.paymentStatus()).isEqualTo("SUCCESS");
                });
    }

    @Test
    @DisplayName("카드 결제 성공 시 데이터 플랫폼 이벤트가 발행된다")
    void shouldPublishDataPlatformEventsOnCardPaymentSuccess() throws InterruptedException {
        // Given: 카드 결제 주문 생성
        CardInfo cardInfo = new CardInfo("SAMSUNG", "1234-5678-9012-3456");
        OrderInfo orderInfo = new OrderInfo(
                List.of(new OrderItemInfo(testProduct.productId(), 1)),
                null, // 쿠폰 없음
                "CARD",
                cardInfo
        );

        // 카드 결제 주문 생성 (PENDING 상태)
        Order order = orderFacade.placeOrder(testUser.id(), orderInfo);
        
        // Payment 조회 (transactionKey로 콜백 시뮬레이션)
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        String originalTransactionKey = payment.getTransactionKey();
        
        // PG 실패 시 transactionKey가 null일 수 있으므로 테스트용 키 생성
        final String transactionKey;
        if (originalTransactionKey == null) {
            transactionKey = "test-transaction-" + order.getId();
            payment.updateTransactionKey(transactionKey);
            paymentRepository.save(payment);
        } else {
            transactionKey = originalTransactionKey;
        }

        // When: PG 콜백으로 결제 성공 처리
        PgCallbackRequest successCallback = new PgCallbackRequest(
                transactionKey,
                order.getId().toString(),
                "SUCCESS",
                10000L,
                "결제 성공",
                "SAMSUNG",
                "2024-01-01T12:00:00Z"
        );
        
        paymentCallbackService.handlePaymentCallback(successCallback);
        
        // 비동기 이벤트 처리를 위한 대기
        Thread.sleep(500);

        // Then: 데이터 플랫폼 이벤트들이 발행되었는지 확인
        assertThat(events.stream(OrderDataPlatformEvent.class))
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(order.getId());
                    assertThat(event.userId()).isEqualTo(testUser.id());
                    assertThat(event.orderStatus()).isEqualTo(OrderStatus.PAID.name());
                });

        assertThat(events.stream(PaymentDataPlatformEvent.class))
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(order.getId());
                    assertThat(event.userId()).isEqualTo(testUser.id());
                    assertThat(event.paymentType()).isEqualTo("CARD");
                    assertThat(event.paymentStatus()).isEqualTo("SUCCESS");
                    assertThat(event.transactionKey()).isEqualTo(transactionKey);
                });
    }

    @Test
    @DisplayName("카드 결제 실패 시 데이터 플랫폼 이벤트가 발행된다")
    void shouldPublishDataPlatformEventsOnCardPaymentFailure() throws InterruptedException {
        // Given: 카드 결제 주문 생성
        CardInfo cardInfo = new CardInfo("SAMSUNG", "1234-5678-9012-3456");
        OrderInfo orderInfo = new OrderInfo(
                List.of(new OrderItemInfo(testProduct.productId(), 1)),
                null, // 쿠폰 없음
                "CARD",
                cardInfo
        );

        // 카드 결제 주문 생성 (PENDING 상태)
        Order order = orderFacade.placeOrder(testUser.id(), orderInfo);
        
        // Payment 조회 (transactionKey로 콜백 시뮬레이션)
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        String originalTransactionKey = payment.getTransactionKey();
        
        // PG 실패 시 transactionKey가 null일 수 있으므로 테스트용 키 생성
        final String transactionKey;
        if (originalTransactionKey == null) {
            transactionKey = "test-transaction-" + order.getId();
            payment.updateTransactionKey(transactionKey);
            paymentRepository.save(payment);
        } else {
            transactionKey = originalTransactionKey;
        }

        // When: PG 콜백으로 결제 실패 처리
        PgCallbackRequest failureCallback = new PgCallbackRequest(
                transactionKey,
                order.getId().toString(),
                "FAILED",
                10000L,
                "결제 실패 - 카드 한도 초과",
                "SAMSUNG",
                "2024-01-01T12:00:00Z"
        );
        
        paymentCallbackService.handlePaymentCallback(failureCallback);
        
        // 비동기 이벤트 처리를 위한 대기
        Thread.sleep(500);

        // Then: 데이터 플랫폼 이벤트들이 발행되었는지 확인
        assertThat(events.stream(OrderDataPlatformEvent.class))
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(order.getId());
                    assertThat(event.userId()).isEqualTo(testUser.id());
                    assertThat(event.orderStatus()).isEqualTo(OrderStatus.CANCELLED.name());
                });

        assertThat(events.stream(PaymentDataPlatformEvent.class))
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event.orderId()).isEqualTo(order.getId());
                    assertThat(event.userId()).isEqualTo(testUser.id());
                    assertThat(event.paymentType()).isEqualTo("CARD");
                    assertThat(event.paymentStatus()).isEqualTo("FAILED");
                    assertThat(event.transactionKey()).isEqualTo(transactionKey);
                });
    }

}