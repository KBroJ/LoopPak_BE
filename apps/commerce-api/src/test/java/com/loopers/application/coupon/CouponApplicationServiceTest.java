package com.loopers.application.coupon;

import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.coupon.*;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponApplicationServiceIntegrationTest {

    @Autowired
    private UserApplicationService userAppService;
    @Autowired
    private CouponApplicationService couponAppService;
    @Autowired
    private UserCouponRepository userCouponRepository;
    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private UserInfo testUser;
    private Coupon tenPercentCoupon;
    private Coupon thousandWonCoupon;

    @BeforeEach
    void setUp() {
        // 테스트에 사용할 사용자 및 쿠폰 템플릿 생성
        testUser = userAppService.saveUser("testUser", "MALE", "2000-01-01", "test@test.com");
        tenPercentCoupon = couponRepository.save(Coupon.of("10% 할인", "", CouponType.PERCENTAGE, 10, 100, ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(30)));
        thousandWonCoupon = couponRepository.save(Coupon.of("1000원 할인", "", CouponType.FIXED, 1000, 100, ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(30)));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("사용자에게 쿠폰 발급")
    @Nested
    class IssueCouponToUser {

        @Test
        @DisplayName("사용자에게 쿠폰을 정상적으로 발급하고 DB에서 확인할 수 있다.")
        void issuesCouponSuccessfully_andCanBeFoundInDB() {
            // arrange

            // act
            couponAppService.issueCouponToUser(testUser.userId(), tenPercentCoupon.getId());

            // assert
            Page<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndStatus(
                    testUser.id(), UserCouponStatus.AVAILABLE, PageRequest.of(0, 10)
            );
            assertThat(userCoupons.getTotalElements()).isEqualTo(1);
            assertThat(userCoupons.getContent().get(0).getUserId()).isEqualTo(testUser.id());
            assertThat(userCoupons.getContent().get(0).getCouponId()).isEqualTo(tenPercentCoupon.getId());
        }
    }

    @DisplayName("내 쿠폰 목록 조회")
    @Nested
    class GetMyAvailableCoupons {

        @Test
        @DisplayName("발급된 쿠폰 목록을 조회하면 DTO 페이지가 정상적으로 반환된다.")
        void returnsPagedUserCouponInfo_whenCouponsExist() {
            // arrange
            couponAppService.issueCouponToUser(testUser.userId(), tenPercentCoupon.getId());
            couponAppService.issueCouponToUser(testUser.userId(), thousandWonCoupon.getId());

            // 이미 사용한 쿠폰도 하나 생성
            UserCoupon usedCoupon = UserCoupon.of(testUser.id(), thousandWonCoupon.getId(), ZonedDateTime.now().plusDays(10));
            usedCoupon.use();
            userCouponRepository.save(usedCoupon);

            // act
            Page<UserCouponInfo> result = couponAppService.getMyAvailableCoupons(testUser.id(), PageRequest.of(0, 10));

            // assert
            // '사용 가능한' 쿠폰은 2개이므로, 2개가 조회되어야 함
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).extracting("couponInfo.name")
                    .containsExactlyInAnyOrder("10% 할인", "1000원 할인");
        }
    }
}
