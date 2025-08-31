package com.loopers.interfaces.api.payment;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.*;
import com.loopers.domain.product.ProductStatus;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgPaymentStatus;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentV1ApiE2ETest {

    @Autowired private TestRestTemplate testRestTemplate;
    @Autowired private DatabaseCleanUp databaseCleanUp;
    @Autowired private UserApplicationService userAppService;
    @Autowired private BrandApplicationService brandAppService;
    @Autowired private ProductFacade productFacade;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;

    @MockitoBean private PgClient pgClient;

    private UserInfo testUser;
    private ProductResponse testProduct;

    @BeforeEach
    void setUp() {
        testUser = userAppService.saveUser("e2eUser", "M", "2000-01-01", "e2e@test.com");

        BrandInfo brand = brandAppService.create("E2E브랜드", "설명", true);
        testProduct = productFacade.create(brand.id(), "상품1", "", 10000, 20, 10, ProductStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /**
     * E2E 테스트용 Order 생성 헬퍼 메서드 (OrderItem 포함)
     */
    private Order createTestOrderWithItems() {
        java.util.List<OrderItem> orderItems = new java.util.ArrayList<>();
        orderItems.add(OrderItem.of(testProduct.productId(), 1, 10000L));
        Order order = Order.of(testUser.id(), orderItems, 0L, OrderStatus.PENDING);
        return orderRepository.save(order);
    }

    @Test
    @DisplayName("성공: PG 콜백 수신 시 결제와 주문 상태가 정상적으로 업데이트된다.")
    void handlePaymentCallback_e2e_success() {
        // arrange
        String transactionKey = "20250822:TR:e2e_success";
        
        // OrderItem을 포함한 Order 생성
        Order order = createTestOrderWithItems();
        String orderId = String.valueOf(order.getId());

        // 실제 Order ID로 결제 데이터 생성
        Payment payment = Payment.of(order.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 10000L);
        payment.updateTransactionKey(transactionKey);
        paymentRepository.save(payment);

        String callbackUrl = "/api/v1/payments/callback";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        PgCallbackRequest callbackRequest = new PgCallbackRequest(
            transactionKey, orderId, "SUCCESS", 10000L, "결제 성공", "SAMSUNG", "2025-08-22T10:00:00Z"
        );

        // act
        ResponseEntity<ApiResponse<String>> response = testRestTemplate.exchange(
            callbackUrl,
            HttpMethod.POST,
            new HttpEntity<>(callbackRequest, headers),
            new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
        assertThat(response.getBody().data()).isEqualTo("결제 콜백 처리 완료");

        // Payment 상태가 SUCCESS로 변경되었는지 확인
        Payment updatedPayment = paymentRepository.findByTransactionKey(transactionKey).orElse(null);
        assertThat(updatedPayment).isNotNull();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Order 상태가 PAID로 변경되었는지 확인
        Order updatedOrder = orderRepository.findByIdWithItems(order.getId()).orElse(null);
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("실패: PG 콜백에서 FAILED 상태 수신 시 결제는 FAILED, 주문은 CANCELLED로 변경된다.")
    void handlePaymentCallback_e2e_failed() {
        // arrange
        String transactionKey = "20250822:TR:e2e_failed";
        
        // OrderItem을 포함한 Order 생성
        Order order = createTestOrderWithItems();
        String orderId = String.valueOf(order.getId());

        // 실제 Order ID로 결제 데이터 생성
        Payment payment = Payment.of(order.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 5000L);
        payment.updateTransactionKey(transactionKey);
        paymentRepository.save(payment);

        String callbackUrl = "/api/v1/payments/callback";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        PgCallbackRequest callbackRequest = new PgCallbackRequest(
            transactionKey, orderId, "FAILED", 5000L, "한도초과", "SAMSUNG", "2025-08-22T10:00:00Z"
        );

        // act
        ResponseEntity<ApiResponse<String>> response = testRestTemplate.exchange(
            callbackUrl,
            HttpMethod.POST,
            new HttpEntity<>(callbackRequest, headers),
            new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Payment 상태가 FAILED로 변경되었는지 확인
        Payment updatedPayment = paymentRepository.findByTransactionKey(transactionKey).orElse(null);
        assertThat(updatedPayment).isNotNull();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        // Order 상태가 CANCELLED로 변경되었는지 확인
        Order updatedOrder = orderRepository.findByIdWithItems(order.getId()).orElse(null);
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("성공: 결제 상태 조회 API가 올바른 정보를 반환한다.")
    void checkPaymentStatus_e2e_success() {
        // arrange
        String transactionKey = "20250822:TR:status_check";
        
        // 임시 Order 생성 (상태 조회 테스트에서는 실제 조회하지 않음)
        Order tempOrder = createTestOrderWithItems();

        Payment payment = Payment.of(tempOrder.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 15000L);
        payment.updateTransactionKey(transactionKey);
        payment.markAsSuccess(transactionKey);
        paymentRepository.save(payment);

        String statusUrl = "/api/v1/payments/" + transactionKey + "/status";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", testUser.id().toString());

        // act
        ResponseEntity<ApiResponse<PaymentV1Dto.PaymentStatusResponse>> response = testRestTemplate.exchange(
            statusUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentStatusResponse>>() {}
        );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
        assertThat(response.getBody().data().transactionKey()).isEqualTo(transactionKey);
        assertThat(response.getBody().data().ourStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("성공: 결제 상태 동기화 API가 PG 시스템과 정상적으로 동기화된다.")
    void syncPaymentStatus_e2e_success() {
        // arrange
        String transactionKey = "20250822:TR:sync_test";

        // OrderItem을 포함한 Order 생성
        Order order = createTestOrderWithItems();

        Payment payment = Payment.of(order.getId(), PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111"), 20000L);
        payment.updateTransactionKey(transactionKey);
        paymentRepository.save(payment);

        // PG에서 SUCCESS 상태 응답하도록 Mock 설정
        PgPaymentResponse pgResponse = PgPaymentResponse.success(transactionKey, PgPaymentStatus.SUCCESS);
        when(pgClient.getPayment(anyString(), anyString())).thenReturn(pgResponse);

        String syncUrl = "/api/v1/payments/sync-status";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-USER-ID", testUser.id().toString());

        PaymentV1Dto.PaymentSyncRequest syncRequest = new PaymentV1Dto.PaymentSyncRequest(transactionKey);

        // act
        ResponseEntity<ApiResponse<PaymentV1Dto.PaymentStatusResponse>> response = testRestTemplate.exchange(
            syncUrl,
            HttpMethod.POST,
            new HttpEntity<>(syncRequest, headers),
            new ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentStatusResponse>>() {}
        );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

        // Payment 상태가 동기화되었는지 확인
        Payment updatedPayment = paymentRepository.findByTransactionKey(transactionKey).orElse(null);
        assertThat(updatedPayment).isNotNull();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Order 상태도 연동되었는지 확인
        Order updatedOrder = orderRepository.findByIdWithItems(order.getId()).orElse(null);
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("성공: 콜백 헬스체크 엔드포인트가 정상적으로 응답한다.")
    void callbackHealth_e2e_success() {
        // arrange
        String healthUrl = "/api/v1/payments/callback/health";

        // act
        ResponseEntity<String> response = testRestTemplate.getForEntity(healthUrl, String.class);

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Callback endpoint is healthy");
    }
}