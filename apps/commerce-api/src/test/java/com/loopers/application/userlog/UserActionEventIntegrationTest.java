package com.loopers.application.userlog;

import com.loopers.application.userlog.event.UserActionEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.*;

/**
 * 유저 행동 로깅 시스템의 이벤트 발행/처리 기본 동작 테스트
 *
 * 목적:
 * 1. UserActionEvent가 제대로 발행되는지 확인
 * 2. UserActionEventHandler가 이벤트를 받아서 처리하는지 확인
 * 3. Spring의 이벤트 시스템 연결 상태 검증
 */
@SpringBootTest
@RecordApplicationEvents  // Spring이 발행된 이벤트를 기록하도록 설정
class UserActionEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher; // 이벤트 발행용

    @Autowired
    private ApplicationEvents events; // 발행된 이벤트 확인용

    @Test
    @DisplayName("상품 조회 이벤트가 발행되고 핸들러에서 처리된다")
    void shouldPublishAndHandleProductViewEvent() {
        // Arrange
        Long userId = 1L;
        Long productId = 100L;

        // Act
        UserActionEvent event = UserActionEvent.productView(userId, productId);
        eventPublisher.publishEvent(event);

        // Assert
        assertThat(events.stream(UserActionEvent.class))
                .hasSize(1)  // 1개의 이벤트가 발행되었는지
                .allSatisfy(publishedEvent -> {
                    assertThat(publishedEvent.userId()).isEqualTo(userId);
                    assertThat(publishedEvent.action()).isEqualTo("PRODUCT_VIEW");
                    assertThat(publishedEvent.targetType()).isEqualTo("PRODUCT");
                    assertThat(publishedEvent.targetId()).isEqualTo(productId);
                    assertThat(publishedEvent.details()).isEqualTo("상품 상세 조회");
                    assertThat(publishedEvent.timestamp()).isNotNull();
                });
    }

    @Test
    @DisplayName("상품 검색 이벤트가 발행되고 올바른 정보가 전달된다")
    void shouldPublishProductSearchEvent() {
        // Arrange
        Long userId = 2L;
        String query = "노트북";
        String sort = "price_asc";
        int totalResults = 15;

        // Act
        UserActionEvent event = UserActionEvent.productSearch(userId, query, sort, totalResults);
        eventPublisher.publishEvent(event);

        // Assert
        assertThat(events.stream(UserActionEvent.class))
                .hasSize(1)
                .allSatisfy(publishedEvent -> {
                    assertThat(publishedEvent.userId()).isEqualTo(userId);
                    assertThat(publishedEvent.action()).isEqualTo("PRODUCT_SEARCH");
                    assertThat(publishedEvent.targetType()).isEqualTo("SEARCH");
                    assertThat(publishedEvent.targetId()).isNull(); // 검색은 특정 ID가 없음
                    assertThat(publishedEvent.details()).contains(query, sort, "15");
                });
    }

    @Test
    @DisplayName("좋아요 추가 이벤트가 발행되고 올바른 정보가 전달된다")
    void shouldPublishLikeAddedEvent() {
        // Arrange
        Long userId = 3L;
        Long productId = 200L;

        // Act
        UserActionEvent event = UserActionEvent.likeAction(userId, productId, "PRODUCT", true);
        eventPublisher.publishEvent(event);

        // Assert
        assertThat(events.stream(UserActionEvent.class))
                .hasSize(1)
                .allSatisfy(publishedEvent -> {
                    assertThat(publishedEvent.userId()).isEqualTo(userId);
                    assertThat(publishedEvent.action()).isEqualTo("LIKE_ADDED");
                    assertThat(publishedEvent.targetType()).isEqualTo("PRODUCT");
                    assertThat(publishedEvent.targetId()).isEqualTo(productId);
                    assertThat(publishedEvent.details()).contains("좋아요 추가");
                });
    }

}