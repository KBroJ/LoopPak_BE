package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderRequest;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.points.PointModel;
import com.loopers.domain.points.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderUsecaseIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    // --- 테스트 데이터 준비를 위한 의존성 ---
    @Autowired
    private UserService userService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private ProductService productService;
    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserModel testUser;
    private Product product1, product2;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 사용자 생성
        testUser = userService.saveUser("testuser", "MALE", "2000-01-01", "test@test.com");

        // 2. 브랜드 및 상품 생성
        Brand brand = brandService.create(Brand.of("테스트브랜드", "", true));
        product1 = productService.create(Product.of(brand.getId(), "상품1", "", 10000, 10, 10, ProductStatus.ACTIVE));
        product2 = productService.create(Product.of(brand.getId(), "상품2", "", 5000, 10, 10, ProductStatus.ACTIVE));

        // 3. 주문에 필요한 포인트 충전
        pointService.chargePoint(testUser.getUserId(), 100000L);

        // 4. 테스트용 주문 생성 (Facade를 통해 실제 주문 실행)
        OrderRequest orderRequest = new OrderRequest(List.of(
                new OrderItemRequest(product1.getId(), 2), // 10000 * 2 = 20000
                new OrderItemRequest(product2.getId(), 1)  // 5000 * 1 = 5000
        ));
        savedOrder = orderFacade.placeOrder(testUser.getId(), orderRequest);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성")
    @Nested
    class PlaceOrder {

        @DisplayName("정상적으로 주문 생성 시, 재고와 포인트가 올바르게 차감된다.")
        @Test
        void succeedsAndChangesStateCorrectly_whenPlacingOrder() {
            // arrange
            // @BeforeEach에서 생성된 사용자(testUser), 상품(product1, product2) 사용
            // 초기 재고: 10개, 초기 포인트: 100,000
            long initialPoints = pointService.getPointByUserId(testUser.getId()).getPoint();
            int initialStock = productService.productInfo(product1.getId()).get().getStock();

            OrderRequest orderRequest = new OrderRequest(List.of(
                    new OrderItemRequest(product1.getId(), 2) // 10000 * 2 = 20000
            ));

            // act
            Order newOrder = orderFacade.placeOrder(testUser.getId(), orderRequest);

            // assert
            // 1. 재고와 포인트의 상태 변화를 직접 DB에서 다시 조회하여 검증
            Product updatedProduct = productService.productInfo(product1.getId()).get();
            assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 2);

            PointModel updatedPoint = pointService.getPointByUserId(testUser.getId());
            assertThat(updatedPoint.getPoint()).isEqualTo(initialPoints - 20000L);

            // 2. 반환된 Order 객체 검증
            assertThat(newOrder.getId()).isNotNull();
            assertThat(newOrder.calculateTotalPrice()).isEqualTo(20000L);
        }

        @DisplayName("상품 재고가 부족할 경우, CoreException이 발생하고 롤백된다.")
        @Test
        void failsAndRollsBack_whenStockIsInsufficient() {
            // arrange
            long initialPoints = pointService.getPointByUserId(testUser.getId()).getPoint();
            // 재고(10개)보다 많은 수량(11개)을 주문
            OrderRequest orderRequest = new OrderRequest(List.of(
                    new OrderItemRequest(product1.getId(), 11)
            ));

            // act & assert
            // 1. 예외 발생 검증
            assertThatThrownBy(() -> orderFacade.placeOrder(testUser.getId(), orderRequest))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("재고가 부족합니다");

            // 2. 롤백 검증: 예외 발생 후 포인트가 차감되지 않았는지 확인
            PointModel pointAfterFailure = pointService.getPointByUserId(testUser.getId());
            assertThat(pointAfterFailure.getPoint()).isEqualTo(initialPoints);
        }

        @DisplayName("사용자 포인트가 부족할 경우, CoreException이 발생하고 롤백된다.")
        @Test
        void failsAndRollsBack_whenPointsAreInsufficient() {
            // arrange
            UserModel poorUser = userService.saveUser("poorUser", "F", "2001-01-01", "poor@test.com");
            pointService.chargePoint(poorUser.getUserId(), 15000L); // 15,000 포인트만 충전
            int initialStock = productService.productInfo(product1.getId()).get().getStock();

            // 재고는 충분하지만(2 <= 10), 가격은 보유 포인트보다 비싼(20,000 > 15,000) 주문 생성
            OrderRequest orderRequest = new OrderRequest(List.of(
                    new OrderItemRequest(product1.getId(), 2) // 10000 * 2 = 20000
            ));

            // act & assert
            // 1. 예외 발생 검증
            assertThatThrownBy(() -> orderFacade.placeOrder(poorUser.getId(), orderRequest))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            // 2. 롤백 검증: 예외 발생 후 재고가 차감되지 않았는지 확인
            Product productAfterFailure = productService.productInfo(product1.getId()).get();
            assertThat(productAfterFailure.getStock()).isEqualTo(initialStock);
        }
    }

    @DisplayName("내 주문 목록 조회 시, 요약 정보가 페이징 처리되어 정확히 반환된다.")
    @Test
    void getMyOrders_returnsCorrectOrderSummary() {
        // act
        Page<OrderSummaryResponse> resultPage = orderFacade.getMyOrders(testUser.getId(), 0, 10);

        // assert
        assertThat(resultPage.getTotalElements()).isEqualTo(1);
        assertThat(resultPage.getContent()).hasSize(1);

        OrderSummaryResponse summary = resultPage.getContent().get(0);
        assertThat(summary.orderId()).isEqualTo(savedOrder.getId());
        assertThat(summary.status()).isEqualTo(OrderStatus.PENDING); // Order 엔티티의 초기 상태
        assertThat(summary.totalPrice()).isEqualTo(25000L);
        assertThat(summary.representativeProductName()).isEqualTo("상품 ID:" + product1.getId() + " 외 1건");
    }

    @DisplayName("단일 주문 상세 조회 시, 주문 상품 정보가 모두 포함된 상세 정보가 반환된다.")
    @Test
    void getOrderDetail_returnsCorrectDetailInfo() {
        // act
        OrderDetailResponse result = orderFacade.getOrderDetail(savedOrder.getId());

        // assert
        // 1. 주문 기본 정보 검증
        assertThat(result.orderId()).isEqualTo(savedOrder.getId());
        assertThat(result.totalPrice()).isEqualTo(25000L);

        // 2. 주문 상품 목록 검증
        List<OrderItemResponse> items = result.orderItems();
        assertThat(items).hasSize(2);

        // 3. 각 상품 상세 내역 검증 (Spot-check)
        OrderItemResponse item1 = items.stream().filter(i -> i.productId().equals(product1.getId())).findFirst().orElseThrow();
        assertThat(item1.productName()).isEqualTo("상품1");
        assertThat(item1.quantity()).isEqualTo(2);
        assertThat(item1.price()).isEqualTo(10000L); // 주문 시점의 가격

        OrderItemResponse item2 = items.stream().filter(i -> i.productId().equals(product2.getId())).findFirst().orElseThrow();
        assertThat(item2.productName()).isEqualTo("상품2");
        assertThat(item2.quantity()).isEqualTo(1);
        assertThat(item2.price()).isEqualTo(5000L);
    }

}
