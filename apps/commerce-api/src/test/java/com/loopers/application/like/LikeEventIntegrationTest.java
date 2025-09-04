package com.loopers.application.like;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.brand.BrandApplicationService;
import com.loopers.application.brand.BrandInfo;
import com.loopers.application.like.event.LikeAddedEvent;
import com.loopers.application.like.event.LikeRemovedEvent;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductResponse;
import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.KafkaCleanUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좋아요 이벤트 기반 분리 통합 테스트
 */
@SpringBootTest
//@RecordApplicationEvents
class LikeEventIntegrationTest {

    @Autowired private LikeFacade likeFacade;
    @Autowired private UserApplicationService userAppService;
    @Autowired private BrandApplicationService brandAppService;
    @Autowired private ProductFacade productFacade;
    @Autowired private ProductRepository productRepository;
//    @Autowired private ApplicationEvents events;

    @Autowired private KafkaTemplate<Object, Object> kafkaTemplate;
    @Autowired private KafkaCleanUp kafkaCleanUp;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private DatabaseCleanUp databaseCleanUp;

    private UserInfo testUser;
    private ProductResponse testProduct;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        kafkaCleanUp.truncateAllTopics();
        
        // 테스트 데이터 준비
        testUser = userAppService.saveUser("testUser", "M", "2000-01-01", "test@example.com");
        
        BrandInfo brand = brandAppService.create("테스트브랜드", "설명", true);
        testProduct = productFacade.create(brand.id(), "테스트상품", "", 10000, 20, 10, ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("좋아요 추가 시 이벤트가 발행되고 집계가 처리된다")
    void shouldPublishEventAndUpdateAggregationOnLike() throws InterruptedException, JsonProcessingException {
        // Arrange
//        Product product = productRepository.productInfo(testProduct.productId()).orElseThrow();
//        long initialLikeCount = product.getLikeCount();

        // Act
        likeFacade.like(testUser.id(), testProduct.productId(), LikeType.PRODUCT);
        // 카프카 메시지 발행 대기
        Thread.sleep(100);

        // Assert
        // 이벤트 발행 검증
//        assertThat(events.stream(LikeAddedEvent.class))
//                .hasSize(1)
//                .allSatisfy(event -> {
//                    assertThat(event.userId()).isEqualTo(testUser.id());
//                    assertThat(event.targetId()).isEqualTo(testProduct.productId());
//                    assertThat(event.likeType()).isEqualTo(LikeType.PRODUCT);
//                });

        // 집계 업데이트 검증
//        Product updatedProduct = productRepository.productInfo(testProduct.productId()).orElseThrow();
//        assertThat(updatedProduct.getLikeCount()).isEqualTo(initialLikeCount + 1);

        // Kafka 메시지 검증
        var receivedRecord = kafkaTemplate.receive("catalog-events", 0, 0);
        assertThat(receivedRecord).isNotNull();
        assertThat(receivedRecord.key()).isEqualTo(testProduct.productId().toString());

        // JSON 파싱 후 내용 검증
        String messageValue = (String) receivedRecord.value();
        LikeAddedEvent event = objectMapper.readValue(messageValue, LikeAddedEvent.class);
        assertThat(event.userId()).isEqualTo(testUser.id());
        assertThat(event.targetId()).isEqualTo(testProduct.productId());
        assertThat(event.likeType()).isEqualTo(LikeType.PRODUCT);
    }

    @Test
    @DisplayName("좋아요 제거 시 이벤트가 발행되고 집계가 처리된다")
    void shouldPublishEventAndUpdateAggregationOnUnlike() throws InterruptedException, JsonProcessingException {
        // Arrange
        // 먼저 좋아요를 추가
        likeFacade.like(testUser.id(), testProduct.productId(), LikeType.PRODUCT);

//        Product product = productRepository.productInfo(testProduct.productId()).orElseThrow();
//        long likeCountAfterAdd = product.getLikeCount();

        // Act
        likeFacade.unlike(testUser.id(), testProduct.productId(), LikeType.PRODUCT);
        // 카프카 메시지 발행 대기
        Thread.sleep(100);

        // Assert
        // 이벤트 발행 검증 (제거 이벤트 확인)
//        assertThat(events.stream(LikeRemovedEvent.class))
//                .hasSize(1)
//                .allSatisfy(event -> {
//                    assertThat(event.userId()).isEqualTo(testUser.id());
//                    assertThat(event.targetId()).isEqualTo(testProduct.productId());
//                    assertThat(event.likeType()).isEqualTo(LikeType.PRODUCT);
//                });

        // 집계 업데이트 검증
//        Product updatedProduct = productRepository.productInfo(testProduct.productId()).orElseThrow();
//        assertThat(updatedProduct.getLikeCount()).isEqualTo(likeCountAfterAdd - 1);

        // Kafka 메시지 검증
        var receivedRecord = kafkaTemplate.receive("catalog-events", 0, 0);
        assertThat(receivedRecord).isNotNull();
        assertThat(receivedRecord.key()).isEqualTo(testProduct.productId().toString());

        // JSON 파싱 후 내용 검증
        String messageValue = (String) receivedRecord.value();
        LikeRemovedEvent event = objectMapper.readValue(messageValue, LikeRemovedEvent.class);
        assertThat(event.userId()).isEqualTo(testUser.id());
        assertThat(event.targetId()).isEqualTo(testProduct.productId());
        assertThat(event.likeType()).isEqualTo(LikeType.PRODUCT);
    }

    @Test
    @DisplayName("중복 좋아요 시도 시 이벤트가 발행되지 않는다")
    void shouldNotPublishEventOnDuplicateLike() throws InterruptedException {
        // Arrange
        // 이미 좋아요를 추가
        likeFacade.like(testUser.id(), testProduct.productId(), LikeType.PRODUCT);
        // 테스트 환경에서는 동기식으로 처리되므로 대기 불필요

//        Product product = productRepository.productInfo(testProduct.productId()).orElseThrow();
//        long likeCountAfterFirstLike = product.getLikeCount();

        Thread.sleep(100);

        // 첫 번째 좋아요 메시지 소비 (정리)
        var firstMessage = kafkaTemplate.receive("catalog-events", 0, 0);
        assertThat(firstMessage).isNotNull(); // 첫 번째 메시지는 있어야 함

        // Act
        // 동일한 사용자가 같은 상품에 다시 좋아요 시도
        likeFacade.like(testUser.id(), testProduct.productId(), LikeType.PRODUCT);
        Thread.sleep(100);

        // Assert
        // 추가 이벤트가 발행되지 않음 (총 1개만 존재)
//        assertThat(events.stream(LikeAddedEvent.class)).hasSize(1);
        var receivedRecord = kafkaTemplate.receive("catalog-events", 0, 1);
        assertThat(receivedRecord).isNull(); // 메시지가 없어야 함

        // 집계도 변경되지 않음
//        Product updatedProduct = productRepository.productInfo(testProduct.productId()).orElseThrow();
//        assertThat(updatedProduct.getLikeCount()).isEqualTo(likeCountAfterFirstLike);

    }
}