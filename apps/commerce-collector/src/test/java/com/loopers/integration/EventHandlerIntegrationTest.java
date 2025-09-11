package com.loopers.integration;

import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.domain.eventlog.EventLogRepository;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.application.eventhandler.LikeEventHandler;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("EventHandler 통합 테스트 (멱등성 포함)")
class EventHandlerIntegrationTest {

    @Autowired
    private LikeEventHandler likeEventHandler;

    @Autowired
    private EventHandledRepository eventHandledRepository;
    @Autowired
    private EventLogRepository eventLogRepository;
    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("LikeEventHandler 멱등성 테스트")
    @Nested
    class LikeEventHandlerIdempotencyTest {

        @Test
        @DisplayName("같은 eventId로 LikeAddedEvent를 두 번 처리해도 한 번만 반영된다")
        void handleDuplicateLikeAddedEvent_ShouldProcessOnlyOnce() {
            // arrange
            String eventType = "LikeAddedEvent";
            String eventId = "like-event-001";

            // Handler가 받는 형태의 payloadJson (eventId 포함)
            String payloadJson = """
                  {
                      "eventId": "%s",
                      "userId": "user123",
                      "productId": 1,
                      "timestamp": "2024-01-01T10:00:00"
                  }
                  """.formatted(eventId);
            String messageKey = "1"; // productId

            // act - 첫 번째 처리
            likeEventHandler.handle(eventType, payloadJson, messageKey);

            var metricsAfterFirst = productMetricsRepository.findByProductId(1L);

            // act - 두 번째 처리 (중복)
            likeEventHandler.handle(eventType, payloadJson, messageKey);

            var metricsAfterSecond = productMetricsRepository.findByProductId(1L);

            // assert - 멱등성 검증
            assertThat(eventHandledRepository.existsByEventId(eventId)).isTrue();

            assertThat(metricsAfterFirst).isPresent();
            assertThat(metricsAfterSecond).isPresent();

            // 멱등성: 좋아요 수가 1로 유지됨 (중복 처리 안됨)
            assertThat(metricsAfterFirst.get().getLikeCount()).isEqualTo(1);
            assertThat(metricsAfterSecond.get().getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("LikeAddedEvent → LikeRemovedEvent 순서 처리가 올바르게 동작한다")
        void handleLikeAddedAndRemovedEvents_ShouldProcessCorrectly() {
            // arrange - 좋아요 추가
            String addEventType = "LikeAddedEvent";
            String addEventId = "like-add-001";
            String addPayloadJson = """
                  {
                      "eventId": "%s",
                      "userId": "user123",
                      "productId": 1,
                      "timestamp": "2024-01-01T10:00:00"
                  }
                  """.formatted(addEventId);

            // act - 좋아요 추가
            likeEventHandler.handle(addEventType, addPayloadJson, "1");

            // arrange - 좋아요 제거
            String removeEventType = "LikeRemovedEvent";
            String removeEventId = "like-remove-001";
            String removePayloadJson = """
                  {
                      "eventId": "%s",
                      "userId": "user123",
                      "productId": 1,
                      "timestamp": "2024-01-01T10:01:00"
                  }
                  """.formatted(removeEventId);

            // act - 좋아요 제거
            likeEventHandler.handle(removeEventType, removePayloadJson, "1");

            // assert
            assertThat(eventHandledRepository.existsByEventId(addEventId)).isTrue();
            assertThat(eventHandledRepository.existsByEventId(removeEventId)).isTrue();

            var finalMetrics = productMetricsRepository.findByProductId(1L);
            assertThat(finalMetrics).isPresent();
            assertThat(finalMetrics.get().getLikeCount()).isEqualTo(0); // 추가 → 제거로 0이 됨
        }
    }

    @DisplayName("이벤트 처리 결과 검증")
    @Nested
    class EventProcessingTest {

        @Test
        @DisplayName("LikeAddedEvent 처리 시 모든 데이터가 올바르게 저장된다")
        void handleLikeAddedEvent_ShouldSaveCorrectData() {
            // arrange
            String eventId = "like-save-test-001";
            String payloadJson = """
                  {
                      "eventId": "%s",
                      "userId": "user123",
                      "productId": 1,
                      "timestamp": "2024-01-01T10:00:00"
                  }
                  """.formatted(eventId);

            // act
            likeEventHandler.handle("LikeAddedEvent", payloadJson, "1");

            // assert
            // 1. EventHandled 저장 확인
            assertThat(eventHandledRepository.existsByEventId(eventId)).isTrue();

            // 2. EventLog 저장 확인
            var eventLogs = eventLogRepository.findByAggregateId("1");
            assertThat(eventLogs).isNotEmpty();
            assertThat(eventLogs.get(0).getEventType()).isEqualTo("LikeAddedEvent");

            // 3. ProductMetrics 업데이트 확인
            var savedMetrics = productMetricsRepository.findByProductId(1L);
            assertThat(savedMetrics).isPresent();
            assertThat(savedMetrics.get().getLikeCount()).isEqualTo(1);
        }
    }
}
