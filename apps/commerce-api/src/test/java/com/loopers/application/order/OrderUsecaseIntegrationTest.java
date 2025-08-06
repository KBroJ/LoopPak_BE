package com.loopers.application.order;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.points.PointApplicationService;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderRequest;
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
    private UserApplicationService userApplicationService;
    @Autowired
    private BrandApplicationService brandApplicationService;
    @Autowired
    private ProductService productService;
    @Autowired
    private PointApplicationService pointApplicationService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserInfo testUser;
    private Product product1, product2;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        testUser = userApplicationService.saveUser("testuser", "MALE", "2000-01-01", "test@test.com");

        BrandInfo brand = brandApplicationService.create("테스트브랜드", "", true);
        product1 = productService.create(Product.of(brand.id(), "상품1", "", 10000, 10, 10, ProductStatus.ACTIVE));
        product2 = productService.create(Product.of(brand.id(), "상품2", "", 5000, 10, 10, ProductStatus.ACTIVE));

        pointApplicationService.chargePoint(testUser.userId(), 100000L);

        OrderRequest orderRequest = new OrderRequest(List.of(
                new OrderItemRequest(product1.getId(), 2),
                new OrderItemRequest(product2.getId(), 1)
        ));
        savedOrder = orderFacade.placeOrder(testUser.id(), orderRequest);
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
            long initialPoints = pointApplicationService.getPointByUserId(testUser.id()).getPoint();
            int initialStock = productService.productInfo(product1.getId()).get().getStock();

            OrderRequest orderRequest = new OrderRequest(List.of(
                    new OrderItemRequest(product1.getId(), 2)
            ));

            // act
            Order newOrder = orderFacade.placeOrder(testUser.id(), orderRequest);

            // assert
            Product updatedProduct = productService.productInfo(product1.getId()).get();
            assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 2);

            Point updatedPoint = pointApplicationService.getPointByUserId(testUser.id());
            assertThat(updatedPoint.getPoint()).isEqualTo(initialPoints - 20000L);

            assertThat(newOrder.getId()).isNotNull();
            assertThat(newOrder.calculateTotalPrice()).isEqualTo(20000L);
        }

        @DisplayName("상품 재고가 부족할 경우, CoreException이 발생하고 롤백된다.")
        @Test
        void failsAndRollsBack_whenStockIsInsufficient() {
            // arrange
            long initialPoints = pointApplicationService.getPointByUserId(testUser.id()).getPoint();
            OrderRequest orderRequest = new OrderRequest(List.of(
                    new OrderItemRequest(product1.getId(), 11)
            ));

            // act & assert
            assertThatThrownBy(() -> orderFacade.placeOrder(testUser.id(), orderRequest))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("재고가 부족합니다");

            Point pointAfterFailure = pointApplicationService.getPointByUserId(testUser.id());
            assertThat(pointAfterFailure.getPoint()).isEqualTo(initialPoints);
        }

        @DisplayName("사용자 포인트가 부족할 경우, CoreException이 발생하고 롤백된다.")
        @Test
        void failsAndRollsBack_whenPointsAreInsufficient() {
            // arrange
            UserInfo poorUser = userApplicationService.saveUser("poorUser", "F", "2001-01-01", "poor@test.com");
            pointApplicationService.chargePoint(poorUser.userId(), 15000L);
            int initialStock = productService.productInfo(product1.getId()).get().getStock();

            OrderRequest orderRequest = new OrderRequest(List.of(
                    new OrderItemRequest(product1.getId(), 2)
            ));

            // act & assert
            assertThatThrownBy(() -> orderFacade.placeOrder(poorUser.id(), orderRequest))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            Product productAfterFailure = productService.productInfo(product1.getId()).get();
            assertThat(productAfterFailure.getStock()).isEqualTo(initialStock);
        }
    }

    @DisplayName("내 주문 목록 조회 시, 요약 정보가 페이징 처리되어 정확히 반환된다.")
    @Test
    void getMyOrders_returnsCorrectOrderSummary() {
        // act
        Page<OrderSummaryResponse> resultPage = orderFacade.getMyOrders(testUser.id(), 0, 10);

        // assert
        assertThat(resultPage.getTotalElements()).isEqualTo(1);
        assertThat(resultPage.getContent()).hasSize(1);

        OrderSummaryResponse summary = resultPage.getContent().get(0);
        assertThat(summary.orderId()).isEqualTo(savedOrder.getId());
        assertThat(summary.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(summary.totalPrice()).isEqualTo(25000L);
        assertThat(summary.representativeProductName()).isEqualTo("상품 ID:" + product1.getId() + " 외 1건");
    }

    @DisplayName("단일 주문 상세 조회 시, 주문 상품 정보가 모두 포함된 상세 정보가 반환된다.")
    @Test
    void getOrderDetail_returnsCorrectDetailInfo() {
        // act
        OrderDetailResponse result = orderFacade.getOrderDetail(savedOrder.getId());

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
