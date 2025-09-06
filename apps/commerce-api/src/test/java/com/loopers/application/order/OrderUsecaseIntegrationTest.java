package com.loopers.application.order;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.points.PointApplicationService;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.points.Point;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
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
    @Autowired
    private OrderQueryService orderQueryService;
    @Autowired
    private UserApplicationService userAppService;
    @Autowired
    private BrandApplicationService brandAppService;
    @Autowired
    private ProductService productService;
    @Autowired
    private PointApplicationService pointAppService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserInfo testUser;
    private Product product1, product2;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        // 타임스탬프의 마지막 6자리만 사용 (영문+숫자, 10자 이내)
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7); // 뒤 6자리
        String uniqueUserId = "test" + timestamp; // test123456 형태 (10자 이내)
        String uniqueEmail = "test" + timestamp + "@test.com";
        testUser = userAppService.saveUser(uniqueUserId, "MALE", "2000-01-01", uniqueEmail);

        BrandInfo brand = brandAppService.create("테스트브랜드", "", true);
        product1 = productService.create(Product.of(brand.id(), "상품1", "", 10000, 10, 10, ProductStatus.ACTIVE));
        product2 = productService.create(Product.of(brand.id(), "상품2", "", 5000, 10, 10, ProductStatus.ACTIVE));

        pointAppService.chargePoint(testUser.userId(), 100000L);

        OrderInfo orderInfo = new OrderInfo(
            List.of(
                new OrderItemInfo(product1.getId(), 2),
                new OrderItemInfo(product2.getId(), 1)
            ),
            null,
            "POINT", // 기본 결제 방식은 포인트
            null                // 포인트 결제시 PaymentMethod는 null
        );
        savedOrder = orderFacade.placeOrder(testUser.userId(), orderInfo);
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
            long initialPoints = pointAppService.getPointByUserId(testUser.id()).getPoint();
            int initialStock = productService.productInfo(product1.getId()).get().getStock();

            OrderInfo orderInfo = new OrderInfo(
                List.of(new OrderItemInfo(product1.getId(), 2)),
                null,
                "POINT", // 포인트 결제
                null                // 포인트 결제시 PaymentMethod는 null
            );

            // act
            Order newOrder = orderFacade.placeOrder(testUser.userId(), orderInfo);

            // assert
            Product updatedProduct = productService.productInfo(product1.getId()).get();
            assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 2);

            Point updatedPoint = pointAppService.getPointByUserId(testUser.id());
            assertThat(updatedPoint.getPoint()).isEqualTo(initialPoints - 20000L);

            assertThat(newOrder.getId()).isNotNull();
            assertThat(newOrder.calculateTotalPrice()).isEqualTo(20000L);
        }

        @DisplayName("상품 재고가 부족할 경우, CoreException이 발생하고 롤백된다.")
        @Test
        void failsAndRollsBack_whenStockIsInsufficient() {
            // arrange
            long initialPoints = pointAppService.getPointByUserId(testUser.id()).getPoint();
            OrderInfo orderInfo = new OrderInfo(
                List.of(new OrderItemInfo(product1.getId(), 11)),
                null,
                "POINT", // 포인트 결제
                null                // 포인트 결제시 PaymentMethod는 null
            );

            // act & assert
            assertThatThrownBy(() -> orderFacade.placeOrder(testUser.userId(), orderInfo))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("재고가 부족합니다");

            Point pointAfterFailure = pointAppService.getPointByUserId(testUser.id());
            assertThat(pointAfterFailure.getPoint()).isEqualTo(initialPoints);
        }

        @DisplayName("사용자 포인트가 부족할 경우, CoreException이 발생하고 롤백된다.")
        @Test
        void failsAndRollsBack_whenPointsAreInsufficient() {
            // arrange
            UserInfo poorUser = userAppService.saveUser("poorUser", "F", "2001-01-01", "poor@test.com");
            pointAppService.chargePoint(poorUser.userId(), 15000L);
            int initialStock = productService.productInfo(product1.getId()).get().getStock();

            OrderInfo orderRequest = new OrderInfo(
                List.of(new OrderItemInfo(product1.getId(), 2)),
                null,
                "POINT", // 포인트 결제
                null                // 포인트 결제시 PaymentMethod는 null
            );

            // act & assert
            assertThatThrownBy(() -> orderFacade.placeOrder(poorUser.userId(), orderRequest))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            Product productAfterFailure = productService.productInfo(product1.getId()).get();
            assertThat(productAfterFailure.getStock()).isEqualTo(initialStock);
        }
    }

    @DisplayName("내 주문 목록 조회 시, 요약 정보가 페이밍 처리되어 정확히 반환된다.")
    @Test
    void getMyOrders_returnsCorrectOrderSummary() {
        // act
        Page<OrderSummaryResponse> resultPage = orderQueryService.getMyOrders(testUser.userId(), 0, 10);

        // assert
        assertThat(resultPage.getTotalElements()).isEqualTo(1);
        assertThat(resultPage.getContent()).hasSize(1);

        OrderSummaryResponse summary = resultPage.getContent().get(0);
        assertThat(summary.orderId()).isEqualTo(savedOrder.getId());
        assertThat(summary.status()).isEqualTo(OrderStatus.PAID);
        assertThat(summary.totalPrice()).isEqualTo(25000L);
        assertThat(summary.representativeProductName()).isEqualTo("상품 ID:" + product1.getId() + " 외 1건");
    }

    @DisplayName("단일 주문 상세 조회 시, 주문 상품 정보가 모두 포함된 상세 정보가 반환된다.")
    @Test
    void getOrderDetail_returnsCorrectDetailInfo() {
        // act
        OrderDetailResponse result = orderQueryService.getOrderDetail(savedOrder.getId());

        // assert
        assertThat(result.orderId()).isEqualTo(savedOrder.getId());
        assertThat(result.totalPrice()).isEqualTo(25000L);

        List<OrderItemResponse> items = result.orderItems();
        assertThat(items).hasSize(2);

        OrderItemResponse item1 = items.stream().filter(i -> i.productId().equals(product1.getId())).findFirst().orElseThrow();
        assertThat(item1.productName()).isEqualTo("상품1");
        assertThat(item1.quantity()).isEqualTo(2);
        assertThat(item1.price()).isEqualTo(10000L);

        OrderItemResponse item2 = items.stream().filter(i -> i.productId().equals(product2.getId())).findFirst().orElseThrow();
        assertThat(item2.productName()).isEqualTo("상품2");
        assertThat(item2.quantity()).isEqualTo(1);
        assertThat(item2.price()).isEqualTo(5000L);
    }

}
