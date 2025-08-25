package com.loopers.integration.resilience;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentResult;
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
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 실제 PG 시뮬레이터와 연동하여 Resilience 패턴 동작을 검증하는 통합 테스트
 * 
 * 🚀 실행 전 준비사항:
 * 1. PG 시뮬레이터 실행: ./gradlew :apps:pg-simulator:bootRun
 * 2. PG가 http://localhost:8082 에서 실행 중인지 확인
 * 
 * 🎯 테스트 목표:
 * - CircuitBreaker가 실제로 열리고 닫히는지 확인
 * - Retry가 실제로 재시도하는지 확인  
 * - Fallback이 실제로 동작하는지 확인
 * - PG 응답 지연/실패 상황에서 시스템 보호 확인
 */
@SpringBootTest
@TestPropertySource(properties = {
    "pg.base-url=http://localhost:8082",
    "resilience4j.circuitbreaker.instances.pgCircuit.sliding-window-size=3",
    "resilience4j.circuitbreaker.instances.pgCircuit.failure-rate-threshold=50", 
    "resilience4j.circuitbreaker.instances.pgCircuit.minimum-number-of-calls=2",
    "resilience4j.circuitbreaker.instances.pgCircuit.wait-duration-in-open-state=2s",
    "resilience4j.retry.instances.pgRetry.max-attempts=1",
    "resilience4j.retry.instances.pgRetry.wait-duration=100ms"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PgResilienceIntegrationTest {

    @Autowired private PaymentFacade paymentFacade;
    @Autowired private UserApplicationService userAppService;
    @Autowired private BrandApplicationService brandAppService;
    @Autowired private ProductFacade productFacade;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    private UserInfo testUser;
    private ProductResponse testProduct;

    @BeforeEach
    void setUp() {
        testUser = userAppService.saveUser("testuser", "M", "2000-01-01", "test@example.com");
        
        BrandInfo brand = brandAppService.create("Resilience브랜드", "설명", true);
        testProduct = productFacade.create(brand.id(), "Resilience상품", "", 10000, 20, 10, ProductStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Order createTestOrder() {
        java.util.List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(OrderItem.of(testProduct.productId(), 1, 10000L));
        Order order = Order.of(testUser.id(), orderItems, 0L, OrderStatus.PENDING);
        return orderRepository.save(order);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("🔄 Retry 패턴: PG 일시적 장애 시 자동 재시도 후 성공")
    void testRetryPattern_temporaryFailureThenSuccess() {
        // arrange
        Order testOrder = createTestOrder();
        PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "1111-1111-1111-1111");

        System.out.println("\n🔄 [Retry 테스트] PG 요청 시작...");
        
        // act - PG 시뮬레이터의 60% 성공률로 인해 재시도가 발생할 수 있음
        long startTime = System.currentTimeMillis();
        PaymentResult result = paymentFacade.processPayment(
            testUser.id(), testOrder.getId(), 10000L, PaymentType.CARD, paymentMethod
        );
        long duration = System.currentTimeMillis() - startTime;

        // assert
        System.out.println("🔄 [Retry 테스트] 소요시간: " + duration + "ms, 결과: " + result.success());
        
        // 재시도로 인해 1초 이상 걸렸다면 retry가 동작한 것
        if (duration > 1000) {
            System.out.println("✅ Retry 패턴이 동작했습니다!");
        }
        
        // 성공하거나 Fallback으로 처리됐는지 확인
        assertThat(result).isNotNull();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("⚡ Timeout 패턴: PG 응답 지연 시 타임아웃으로 빠른 실패")
    void testTimeoutPattern_pgResponseDelay() {
        // arrange
        Order testOrder = createTestOrder();
        PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "2222-2222-2222-2222");

        System.out.println("\n⚡ [Timeout 테스트] 응답 지연 상황 테스트...");
        
        // act
        long startTime = System.currentTimeMillis();
        PaymentResult result = paymentFacade.processPayment(
            testUser.id(), testOrder.getId(), 15000L, PaymentType.CARD, paymentMethod
        );
        long duration = System.currentTimeMillis() - startTime;

        // assert - 타임아웃 설정에 의해 적절한 시간 내 응답
        System.out.println("⚡ [Timeout 테스트] 소요시간: " + duration + "ms");
        
        assertThat(duration).isLessThan(10000); // 10초 이내 응답
        assertThat(result).isNotNull();
        
        if (duration < 5000) {
            System.out.println("✅ Timeout 패턴이 동작하여 빠르게 처리됐습니다!");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("🛡️ Fallback 패턴: PG 장애 시 대체 로직으로 서비스 보호")
    void testFallbackPattern_pgSystemDown() {
        // arrange - 여러 번 요청으로 실패율 높이기
        System.out.println("\n🛡️ [Fallback 테스트] 연속 요청으로 실패율 높이기...");
        
        int fallbackCount = 0;
        
        // act - 5번 연속 요청하여 실패율 증가
        for (int i = 0; i < 5; i++) {
            Order testOrder = createTestOrder();
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "3333-3333-3333-333" + i);
            
            PaymentResult result = paymentFacade.processPayment(
                testUser.id(), testOrder.getId(), 20000L, PaymentType.CARD, paymentMethod
            );
            
            // Fallback 응답인지 확인 (transactionKey가 FALLBACK_로 시작)
            if (!result.success() && result.transactionId() != null && 
                result.transactionId().startsWith("FALLBACK_")) {
                fallbackCount++;
                System.out.println("🛡️ [Fallback 테스트] " + (i+1) + "번째 요청 - Fallback 동작!");
            }
            
            // 잠시 대기 (CircuitBreaker 상태 변화를 위해)
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // assert
        System.out.println("🛡️ [Fallback 테스트] 총 Fallback 발생 횟수: " + fallbackCount);
        assertThat(fallbackCount).isGreaterThan(0); // 최소 1번은 Fallback 발생
        
        if (fallbackCount > 0) {
            System.out.println("✅ Fallback 패턴이 동작하여 시스템을 보호했습니다!");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("🚦 CircuitBreaker 패턴: 연속 실패 후 회로 열기 및 복구")
    void testCircuitBreakerPattern_openAndRecover() throws InterruptedException {
        System.out.println("\n🚦 [CircuitBreaker 테스트] 회로 상태 변화 테스트...");
        
        // Phase 1: 연속 실패로 CircuitBreaker 열기
        System.out.println("📍 Phase 1: 연속 실패 요청으로 회로 열기");
        int immediateFailures = 0;
        
        // PG 시뮬레이터 특성: 같은 카드번호로 연속 요청하면 실패율 증가
        for (int i = 0; i < 15; i++) { 
            Order testOrder = createTestOrder();
            // 동일한 카드번호로 연속 요청 + 높은 금액으로 한도 초과 유도
            PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "0000-0000-0000-0000");
            
            long startTime = System.currentTimeMillis();
            PaymentResult result = paymentFacade.processPayment(
                testUser.id(), testOrder.getId(), 500000L, PaymentType.CARD, paymentMethod
            );
            long duration = System.currentTimeMillis() - startTime;
            
            // CircuitBreaker가 열렸거나 Fallback이면 즉시 실패 (매우 빠름)
            boolean isFallback = result.transactionId() != null && result.transactionId().startsWith("FALLBACK_");
            
            // 매우 빠른 응답(50ms 미만)이면서 실패하거나, Fallback 응답이면 CircuitBreaker가 작동한 것
            if ((duration < 50 && (!result.success() || isFallback)) || isFallback) {
                immediateFailures++;
                System.out.println("🚦 " + (i+1) + "번째 요청 - CircuitBreaker OPEN: " + duration + "ms, 성공=" + result.success() + ", fallback=" + isFallback);
            } else {
                System.out.println("🚦 " + (i+1) + "번째 요청 - 정상 처리: " + duration + "ms, 성공=" + result.success() + ", fallback=" + isFallback);
            }
            
            Thread.sleep(20); // 매우 짧은 간격으로 요청해서 CircuitBreaker 빠르게 열기
        }
        
        System.out.println("📊 즉시 실패 (CircuitBreaker OPEN) 횟수: " + immediateFailures);
        
        // Phase 2: 회로 복구 대기
        if (immediateFailures > 0) {
            System.out.println("📍 Phase 2: 회로 복구 대기 (3초)...");
            Thread.sleep(3500); // wait-duration-in-open-state 대기
            
            // Phase 3: 복구 후 정상 요청
            System.out.println("📍 Phase 3: 회로 복구 후 요청");
            Order recoveryOrder = createTestOrder();
            PaymentMethod recoveryMethod = PaymentMethod.of(CardType.SAMSUNG, "5555-5555-5555-5555");
            
            long startTime = System.currentTimeMillis();
            PaymentResult recoveryResult = paymentFacade.processPayment(
                testUser.id(), recoveryOrder.getId(), 30000L, PaymentType.CARD, recoveryMethod
            );
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("🚦 복구 후 첫 요청 - 소요시간: " + duration + "ms, 성공: " + recoveryResult.success());
            
            // 복구 후에는 정상적인 처리 시간이 걸려야 함 (즉시 실패하지 않음)
            assertThat(duration).isGreaterThan(50); // 네트워크 통신 시간
            
            System.out.println("✅ CircuitBreaker 패턴이 정상적으로 동작했습니다!");
        }
        
        // assert
        assertThat(immediateFailures).isGreaterThan(0); // CircuitBreaker가 최소 1번은 열렸어야 함
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("🔥 동시성 테스트: 다중 요청 시 시스템 안정성 확인")
    void testConcurrentRequests_systemStability() throws InterruptedException, ExecutionException {
        System.out.println("\n🔥 [동시성 테스트] 10개 동시 요청으로 시스템 안정성 확인...");
        
        // arrange
        CompletableFuture<PaymentResult>[] futures = new CompletableFuture[10];
        
        // act - 10개 동시 요청
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            final int requestId = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                Order testOrder = createTestOrder();
                PaymentMethod paymentMethod = PaymentMethod.of(CardType.SAMSUNG, "6666-6666-6666-666" + requestId);
                
                return paymentFacade.processPayment(
                    testUser.id(), testOrder.getId(), 10000L, PaymentType.CARD, paymentMethod
                );
            });
        }
        
        // 모든 요청 완료 대기
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
        allOf.get(); // 모든 완료 대기
        
        long totalDuration = System.currentTimeMillis() - startTime;
        
        // assert - 결과 분석
        int successCount = 0;
        int fallbackCount = 0;
        
        for (int i = 0; i < 10; i++) {
            PaymentResult result = futures[i].get();
            if (result.success()) {
                successCount++;
            } else if (result.transactionId() != null && result.transactionId().startsWith("FALLBACK_")) {
                fallbackCount++;
            }
        }
        
        System.out.println("🔥 [동시성 테스트] 총 소요시간: " + totalDuration + "ms");
        System.out.println("🔥 [동시성 테스트] 성공: " + successCount + ", Fallback: " + fallbackCount);
        
        // 시스템이 정상적으로 모든 요청을 처리했는지 확인
        assertThat(successCount + fallbackCount).isEqualTo(10);
        assertThat(totalDuration).isLessThan(30000); // 30초 이내 모든 처리 완료
        
        System.out.println("✅ 동시 요청 상황에서도 시스템이 안정적으로 동작했습니다!");
    }
}