package com.loopers.application.like;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.ProductStatus;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest {

    @Autowired
    private UserApplicationService userAppService;
    @Autowired
    private LikeFacade likeFacade;
    @Autowired
    private ProductFacade productFacade;
    @Autowired
    private BrandApplicationService brandAppService;
    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private ProductResponse product;
    private List<UserInfo> users;

    @BeforeEach
    void setUp() {
        BrandInfo brand = brandAppService.create("브랜드", "설명", true);
        product = productFacade.create(brand.id(), "상품", "", 1000, 100, 10, ProductStatus.ACTIVE);

        users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            users.add(userAppService.saveUser("user" + i, "M", "2000-01-01", "user" + i + "@test.com"));
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("동시에 100명이 동일 상품에 '좋아요'를 눌러도 최종 좋아요 수는 100이다.")
    void likeCount_isCorrect_underConcurrentLikeRequests() throws InterruptedException {
        // arrange
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // act
        for (int i = 0; i < threadCount; i++) {
            final UserInfo user = users.get(i);
            executorService.submit(() -> {
                try {
                    likeFacade.like(user.id(), product.productId(), LikeType.PRODUCT);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // assert
        long finalLikeCount = likeRepository.getLikeCount(product.productId());
        assertThat(finalLikeCount).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("낙관적 락: 동일한 '좋아요'에 대해 동시에 여러 취소 요청이 발생하면, 단 한 번만 처리된다.")
    void optimisticLock_preventsConcurrentUnlike() throws InterruptedException {
        // arrange
        // 모든 스레드가 공격할 단 하나의 '좋아요' 데이터를 생성합니다.
        likeFacade.like(users.get(0).id(), product.productId(), LikeType.PRODUCT);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0); // 성공 카운터
        AtomicInteger failureCount = new AtomicInteger(0); // 실패(락 충돌) 카운터

        // act
        // 10개의 스레드가 모두 동일한 '좋아요'를 취소하려고 시도합니다.
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    likeFacade.unlike(users.get(0).id(), product.productId(), LikeType.PRODUCT);
                    successCount.incrementAndGet(); // 성공 시 카운트 증가
                } catch (ObjectOptimisticLockingFailureException e) {
                    // 낙관적 락 충돌이 발생하면 이곳으로 들어옵니다.
                    failureCount.incrementAndGet(); // 실패 시 카운트 증가
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // assert
        System.out.println("successCount.get() : " + successCount.get());
        System.out.println("failureCount.get() : " + failureCount.get());
        // 단 하나의 스레드만 성공적으로 삭제를 완료해야 합니다.
        assertThat(successCount.get()).isEqualTo(1);
        // 나머지 9개의 스레드는 낙관적 락에 의해 실패(충돌)해야 합니다.
        assertThat(failureCount.get()).isEqualTo(9);
        // 최종적으로 '좋아요' 데이터는 DB에서 삭제되어 없어야 합니다.
        Optional<Like> result = likeRepository.findByUserIdAndTargetIdAndType(users.get(0).id(), product.productId(), LikeType.PRODUCT);
        assertThat(result).isEmpty();
    }

}
