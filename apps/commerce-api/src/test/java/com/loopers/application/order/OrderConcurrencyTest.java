package com.loopers.application.order;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.coupon.CouponApplicationService;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.points.PointApplicationService;
import com.loopers.application.product.ProductApplicationService;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.coupon.UserCouponStatus;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderRequest;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.product.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private UserApplicationService userAppService;
    @Autowired
    private PointApplicationService pointAppService;
    @Autowired
    private BrandApplicationService brandAppService;
    @Autowired
    private ProductApplicationService productAppService;
    @Autowired
    private OrderApplicationService orderAppService;
    @Autowired
    private CouponApplicationService couponAppService;

    @Autowired
    private UserCouponRepository userCouponRepository;
    @Autowired
    private PointRepository pointRepository;

    @Test
    @DisplayName("비관적 락: 동일한 쿠폰으로 동시에 여러 주문을 요청하면, 단 하나의 주문만 성공한다.")
    void pessimisticLock_allowsOnlyOneOrder_forSameCoupon() throws InterruptedException {
        // arrange
        UserInfo user = userAppService.saveUser("user", "M", "2000-01-01", "c-user@test.com");
        BrandInfo brand = brandAppService.create("브랜드", "설명", true);
        ProductResponse product = productAppService.create(brand.id(), "상품", "", 5000, 100, 10, ProductStatus.ACTIVE);
        pointAppService.chargePoint(user.userId(), 100000L);

        // 2. 모든 스레드가 사용할 단 하나의 쿠폰을 생성하고 사용자에게 발급합니다.
        CouponInfo couponTemplate = couponAppService.createCoupon("동시성테스트쿠폰", "", CouponType.FIXED, 1000, 100, ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(30));
        couponAppService.issueCouponToUser(user.userId(), couponTemplate.id());
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndStatus(user.id(), UserCouponStatus.AVAILABLE, PageRequest.of(0,1)).getContent().get(0);

        // 3. 동시성 테스트를 준비합니다.
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 4. 모든 스레드가 동일한 쿠폰 ID를 사용하는 주문 요청을 미리 준비합니다.
        OrderRequest orderRequest = new OrderRequest(
                List.of(new OrderItemRequest(product.productId(), 1)),
                userCoupon.getId()
        );

        // act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderAppService.placeOrder(user.id(), orderRequest);
                    successCount.incrementAndGet(); // 성공 시 카운트 증가
                } catch (Exception e) {
                    // 비관적 락 충돌 시 LockAcquisitionException,
                    // 또는 락 획득 후 상태 변경으로 인한 CoreException 등이 발생할 수 있습니다.
                    System.out.println("주문 실패 (예상된 동작): " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // assert
        assertThat(successCount.get()).as("오직 1개의 요청만 성공해야 함").isEqualTo(1);

        UserCoupon finalCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(finalCoupon.getStatus()).as("쿠폰 상태가 'USED'여야 함").isEqualTo(UserCouponStatus.USED);
    }


    @Test
    @DisplayName("비관적 락: 동일 유저가 동시에 여러 주문을 요청해도, 포인트가 정상적으로 차감된다.")
    void pessimisticLock_deductsPointsCorrectly_forSameUser() throws InterruptedException {
        // arrange
        // 1. 테스트용 사용자, 상품, 포인트를 생성합니다.
        UserInfo user = userAppService.saveUser("pointUser", "F", "2000-01-01", "p-user@test.com");
        BrandInfo brand = brandAppService.create("브랜드", "설명", true);
        ProductResponse product = productAppService.create(brand.id(), "상품", "", 1000, 100, 10, ProductStatus.ACTIVE);
        long initialPoints = 50000L;
        pointAppService.chargePoint(user.userId(), initialPoints);

        // 2. 동시성 테스트를 준비합니다. 10개의 스레드가 각각 1000원짜리 상품 1개를 주문합니다.
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        OrderRequest orderRequest = new OrderRequest(
                List.of(new OrderItemRequest(product.productId(), 1)),
                null // 쿠폰 미사용
        );

        // act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderAppService.placeOrder(user.id(), orderRequest);
                } catch (Exception e) {
                    System.out.println("주문 실패 (포인트 부족 등): " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // assert
        // 3. 최종 포인트를 검증합니다.
        Point finalPoint = pointRepository.findByUserId(user.id()).orElseThrow();
        long expectedPoints = initialPoints - (product.price() * threadCount); // 50000 - (1000 * 10) = 40000

        assertThat(finalPoint.getPoint()).isEqualTo(expectedPoints);
    }

}
