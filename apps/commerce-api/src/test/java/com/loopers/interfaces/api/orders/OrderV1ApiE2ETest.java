package com.loopers.interfaces.api.orders;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.coupon.CouponApplicationService;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.points.PointApplicationService;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductQueryService;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.coupon.UserCouponStatus;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

    @Autowired private TestRestTemplate testRestTemplate;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    @Autowired private UserApplicationService userAppService;
    @Autowired private BrandApplicationService brandAppService;
    @Autowired private ProductFacade productFacade;
    @Autowired private PointApplicationService pointAppService;
    @Autowired private CouponApplicationService couponAppService;
    @Autowired private ProductRepository productRepository;
    @Autowired private PointRepository pointRepository;
    @Autowired private UserCouponRepository userCouponRepository;

    private UserInfo testUser;
    private ProductResponse product1;
    private ProductResponse product2;
    private UserCoupon availableCoupon;

    @BeforeEach
    void setUp() {
        // 주문에 필요한 모든 기본 데이터를 미리 생성
        testUser = userAppService.saveUser("e2eUser", "M", "2000-01-01", "e2e@test.com");
        pointAppService.chargePoint(testUser.userId(), 100000L);

        BrandInfo brand = brandAppService.create("E2E브랜드", "설명", true);
        product1 = productFacade.create(brand.id(), "상품1", "", 10000, 20, 10, ProductStatus.ACTIVE);
        product2 = productFacade.create(brand.id(), "상품2", "", 5000, 20, 10, ProductStatus.ACTIVE);

        CouponInfo couponTemplate = couponAppService.createCoupon("1000원 할인쿠폰", "", CouponType.FIXED, 1000, 100, ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(30));
        couponAppService.issueCouponToUser(testUser.userId(), couponTemplate.id());
        availableCoupon = userCouponRepository.findByUserIdAndStatus(testUser.id(), UserCouponStatus.AVAILABLE, PageRequest.of(0, 10)).getContent().get(0);
    }

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }

    @Test
    @DisplayName("성공: 유효한 요청(재고, 포인트, 쿠폰)으로 주문 시, 2xx 응답과 함께 재고/포인트/쿠폰 상태가 정상적으로 변경된다.")
    void placeOrder_e2e_success() {
        // arrange
        // 1. API 요청 준비 (상품1 2개, 상품2 1개 주문 + 쿠폰 사용)
        String requestUrl = "/api/v1/orders";
        var headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(testUser.id()));

        List<OrderV1Dto.OrderItemRequest> items = List.of(
                new OrderV1Dto.OrderItemRequest(product1.productId(), 2),
                new OrderV1Dto.OrderItemRequest(product2.productId(), 1)
        );
        OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(items, availableCoupon.getId());

        // 2. API 요청 전 초기 상태 기록
        long initialPoints = pointRepository.findByUserId(testUser.id()).get().getPoint();
        int initialStock = productRepository.productInfo(product1.productId()).get().getStock();

        // act
        // 3. API 호출
        ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType = new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

        // assert
        // 4. API 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().orderId()).isNotNull();

        // 5. DB 상태 변경(Side Effect) 검증 - E2E 테스트의 핵심
        // 5-1. 재고 차감 검증
        Product finalProduct = productRepository.productInfo(product1.productId()).get();
        assertThat(finalProduct.getStock()).isEqualTo(initialStock - 2);

        // 5-2. 포인트 차감 검증 (10000*2 + 5000*1 - 쿠폰1000 = 24000점 차감)
        Point finalPoint = pointRepository.findByUserId(testUser.id()).get();
        assertThat(finalPoint.getPoint()).isEqualTo(initialPoints - 24000L);

        // 5-3. 쿠폰 상태 변경 검증
        UserCoupon finalCoupon = userCouponRepository.findById(availableCoupon.getId()).get();
        assertThat(finalCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
    }

    @Test
    @DisplayName("실패: 주문 상품의 재고가 부족할 경우, 4xx 에러를 반환하고 모든 상태는 롤백된다.")
    void placeOrder_e2e_failsAndRollsBack_whenStockIsInsufficient() {
        // arrange
        // 1. API 요청 준비 (재고(20개)보다 많은 21개를 주문)
        String requestUrl = "/api/v1/orders";
        var headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(testUser.id()));

        List<OrderV1Dto.OrderItemRequest> items = List.of(
                new OrderV1Dto.OrderItemRequest(product1.productId(), 21)
        );
        OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(items, availableCoupon.getId());

        // 2. API 요청 전 초기 상태 기록
        long initialPoints = pointRepository.findByUserId(testUser.id()).get().getPoint();
        UserCouponStatus initialCouponStatus = userCouponRepository.findById(availableCoupon.getId()).get().getStatus();

        // act
        // 3. API 호출
        ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

        // assert
        // 4. API 응답 검증 (400 Bad Request 예상)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 5. DB 상태 변경이 없는지(롤백) 검증
        // 5-1. 포인트가 전혀 차감되지 않았는지 확인
        Point finalPoint = pointRepository.findByUserId(testUser.id()).get();
        assertThat(finalPoint.getPoint()).isEqualTo(initialPoints);

        // 5-2. 쿠폰이 사용되지 않은 'AVAILABLE' 상태인지 확인
        UserCoupon finalCoupon = userCouponRepository.findById(availableCoupon.getId()).get();
        assertThat(finalCoupon.getStatus()).isEqualTo(initialCouponStatus);
    }

}
