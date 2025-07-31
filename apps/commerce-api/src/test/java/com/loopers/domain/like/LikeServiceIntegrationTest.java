package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LikeServiceIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        UserModel user = userService.saveUser("유저ID", "MALE", "2025-07-14", "test@test.kr");
        Product product = productService.create(Product.of(
                1l, "상품명", "설명", 100, 10, 10, ProductStatus.ACTIVE
        ));
    }

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }

    @DisplayName("상품 좋아요 등록")
    @Nested
    class addLike {

        @DisplayName("")
        @Test
        void createLike() {

            // arrange


            // act


            // assert

        }

    }

    @DisplayName("상품 좋아요 취소")
    @Nested
    class removeLike {

    }

    @DisplayName("내가 좋아요 한 상품 목록 조회")
    @Nested
    class findMyLikeProduct {

    }

}
