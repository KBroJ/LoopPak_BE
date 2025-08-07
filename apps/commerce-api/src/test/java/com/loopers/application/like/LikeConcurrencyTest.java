package com.loopers.application.like;

import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductApplicationService;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest {

    @Autowired
    private UserApplicationService userAppService;
    @Autowired
    private LikeApplicationService likeAppService;
    @Autowired
    private ProductApplicationService productAppService;
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
        product = productAppService.create(brand.id(), "상품", "", 1000, 100, 10, ProductStatus.ACTIVE);

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
                    likeAppService.like(user.id(), product.productId(), LikeType.PRODUCT);
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

}
