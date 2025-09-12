package com.loopers.consumer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.cache.CacheEvictService;
import com.loopers.application.eventhandled.EventHandledService;
import com.loopers.application.eventhandler.LikeEventHandler;
import com.loopers.application.eventlog.EventLogService;
import com.loopers.application.metrics.MetricsService;
import com.loopers.application.ranking.RankingActionType;
import com.loopers.application.ranking.RankingUpdateMessage;
import com.loopers.common.EventDeserializer;
import com.loopers.infrastructure.kafka.KafkaEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeEventHandlerTest {

    @Mock
    private EventLogService eventLogService;
    @Mock
    private CacheEvictService cacheEvictService;
    @Mock
    private MetricsService metricsService;
    @Mock
    private EventDeserializer eventDeserializer;
    @Mock
    private EventHandledService eventHandledService;
    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LikeEventHandler likeEventHandler;

    @BeforeEach
    void setUp() {
        likeEventHandler = new LikeEventHandler(
                metricsService,
                eventLogService,
                cacheEvictService,
                eventDeserializer,
                eventHandledService,
                objectMapper,
                kafkaEventPublisher
        );
    }

    @DisplayName("지원 이벤트 타입")
    @Nested
    class GetSupportedEventTypes {

        @Test
        @DisplayName("지원하는 이벤트 타입을 반환한다.")
        void returnsSupportedEventTypes() {
            // act
            String[] supportedTypes = likeEventHandler.getSupportedEventTypes();

            // assert
            assertThat(supportedTypes).containsExactly("LikeAddedEvent", "LikeRemovedEvent");
        }
    }

    @DisplayName("좋아요 추가 이벤트 처리")
    @Nested
    class HandleLikeAddedEvent {

        @Test
        @DisplayName("좋아요 추가 이벤트가 성공적으로 처리된다.")
        void handle_ValidLikeAddedEvent_ProcessesSuccessfully() throws Exception {
            // arrange
            String eventType = "LikeAddedEvent";
            String payloadJson = """
                  {
                      "eventId": "like-added-001",
                      "userId": 123,
                      "productId": 456
                  }
                  """;
            String messageKey = "456";
            Object mockLikeEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockLikeEvent);
            when(eventHandledService.isAlreadyHandled("like-added-001"))
                    .thenReturn(false);

            // act
            likeEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 핵심 비즈니스 로직 검증
            verify(eventDeserializer).deserialize(payloadJson, Object.class);
            verify(eventLogService).saveEventLog(mockLikeEvent, "LikeAddedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictTopLikedProductsCache();
            verify(metricsService).increaseLikeCount(456L);

            // 멱등성 관련 검증
            verify(eventHandledService).isAlreadyHandled("like-added-001");
            verify(eventHandledService).markAsHandled("like-added-001", "LikeAddedEvent", "456");
        }

        @Test
        @DisplayName("좋아요 추가 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_LikeAddedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "LikeAddedEvent";
            String payloadJson = """
                  {
                      "eventId": "like-deserialize-fail-001",
                      "userId": 123,
                      "productId": 456
                  }
                  """;
            String messageKey = "456";

            when(eventHandledService.isAlreadyHandled("like-deserialize-fail-001"))
                    .thenReturn(false);
            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> likeEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("역직렬화 실패");

            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }

        @Test
        @DisplayName("중복 좋아요 추가 이벤트는 처리하지 않는다.")
        void handle_DuplicateLikeAddedEvent_SkipsProcessing() {
            // arrange
            String eventType = "LikeAddedEvent";
            String payloadJson = """
                  {
                      "eventId": "like-duplicate-001",
                      "userId": 123,
                      "productId": 456
                  }
                  """;
            String messageKey = "456";

            when(eventHandledService.isAlreadyHandled("like-duplicate-001"))
                    .thenReturn(true); // 이미 처리됨

            // act
            likeEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 비즈니스 로직 호출 안됨
            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
            verify(cacheEvictService, never()).evictProductCache(any());
            verify(metricsService, never()).increaseLikeCount(any());
            verify(eventDeserializer, never()).deserialize(any(), any());
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }
    }

    @DisplayName("좋아요 취소 이벤트 처리")
    @Nested
    class HandleLikeRemovedEvent {

        @Test
        @DisplayName("좋아요 취소 이벤트가 성공적으로 처리된다.")
        void handle_ValidLikeRemovedEvent_ProcessesSuccessfully() throws Exception {
            // arrange
            String eventType = "LikeRemovedEvent";
            String payloadJson = """
                  {
                      "eventId": "like-removed-001",
                      "userId": 123,
                      "productId": 456
                  }
                  """;
            String messageKey = "456";
            Object mockLikeEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockLikeEvent);
            when(eventHandledService.isAlreadyHandled("like-removed-001"))
                    .thenReturn(false);

            // act
            likeEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockLikeEvent, "LikeRemovedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictTopLikedProductsCache();
            verify(metricsService).decreaseLikeCount(456L);

            // 멱등성 관련 검증
            verify(eventHandledService).isAlreadyHandled("like-removed-001");
            verify(eventHandledService).markAsHandled("like-removed-001", "LikeRemovedEvent", "456");
        }

        @Test
        @DisplayName("좋아요 취소 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_LikeRemovedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "LikeRemovedEvent";
            String payloadJson = """
                  {
                      "eventId": "like-removed-fail-001",
                      "userId": 123,
                      "productId": 456
                  }
                  """;
            String messageKey = "456";

            when(eventHandledService.isAlreadyHandled("like-removed-fail-001"))
                    .thenReturn(false);
            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> likeEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("역직렬화 실패");

            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }
    }

    @DisplayName("지원하지 않는 이벤트 타입")
    @Nested
    class HandleUnsupportedEventType {

        @Test
        @DisplayName("지원하지 않는 이벤트 타입일 경우 예외가 발생한다.")
        void handle_UnsupportedEventType_ThrowsIllegalArgumentException() {
            // arrange
            String eventType = "UnsupportedEvent";
            String payloadJson = """
                  {
                      "eventId": "unsupported-001",
                      "data": "test"
                  }
                  """;
            String messageKey = "123";

            when(eventHandledService.isAlreadyHandled("unsupported-001"))
                    .thenReturn(false);

            // act & assert
            assertThatThrownBy(() -> likeEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지원하지 않는 이벤트 타입: UnsupportedEvent");
        }
    }

    @DisplayName("잘못된 메시지 키")
    @Nested
    class HandleInvalidMessageKey {

        @Test
        @DisplayName("잘못된 메시지 키일 경우 예외가 발생한다.")
        void handle_InvalidMessageKey_ThrowsNumberFormatException() {
            // arrange
            String eventType = "LikeAddedEvent";
            String payloadJson = """
                  {
                      "eventId": "like-invalid-001",
                      "userId": 123,
                      "productId": 456
                  }
                  """;
            String messageKey = "invalid";

            when(eventHandledService.isAlreadyHandled("like-invalid-001"))
                    .thenReturn(false);

            // act & assert
            assertThatThrownBy(() -> likeEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("eventId가 없으면 예외가 발생한다")
        @Test
        void handle_MissingEventId_ThrowsException() {
            // arrange
            String eventType = "LikeAddedEvent";
            String messageKey = "456";
            String payloadJson = "{}"; // eventId 없음

            // act & assert
            assertThatThrownBy(() ->
                    likeEventHandler.handle(eventType, payloadJson, messageKey)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("eventId 추출 실패");
        }
    }

    @Test
    @DisplayName("좋아요 추가 시 랭킹 이벤트가 발행된다.")
    void handle_LikeAddedEvent_PublishesRankingEvent() throws Exception {
        // arrange
        String eventType = "LikeAddedEvent";
        String payloadJson = """
          {
              "eventId": "like-added-001",
              "userId": 123,
              "productId": 456
          }
          """;
        String messageKey = "456";

        // Mock 설정
        when(eventHandledService.isAlreadyHandled("like-added-001")).thenReturn(false);
        when(eventDeserializer.deserialize(payloadJson, Object.class)).thenReturn(new Object());

        // act
        likeEventHandler.handle(eventType, payloadJson, messageKey);

        // assert - 랭킹 이벤트 발행 검증
        verify(kafkaEventPublisher).publish(
                eq("ranking-events"),
                eq("456"),
                argThat(message -> {

                    RankingUpdateMessage rankingMessage = (RankingUpdateMessage) message;

                    return  rankingMessage.productId().equals(456L) &&
                            rankingMessage.actionType() == RankingActionType.LIKE_ADDED &&
                            rankingMessage.eventId().equals("like-added-001_RANKING");
                })
        );
    }

    @Test
    @DisplayName("좋아요 취소 시 랭킹 이벤트가 발행된다.")
    void handle_LikeRemovedEvent_PublishesRankingEvent() throws Exception {
        // arrange
        String eventType = "LikeRemovedEvent";
        String payloadJson = """
          {
              "eventId": "like-removed-001",
              "userId": 123,
              "productId": 456
          }
          """;
        String messageKey = "456";

        when(eventHandledService.isAlreadyHandled("like-removed-001")).thenReturn(false);
        when(eventDeserializer.deserialize(payloadJson, Object.class)).thenReturn(new Object());

        // act
        likeEventHandler.handle(eventType, payloadJson, messageKey);

        // assert - 랭킹 이벤트 발행 검증 (LIKE_REMOVED)
        verify(kafkaEventPublisher).publish(
                eq("ranking-events"),
                eq("456"),
                argThat(message -> {
                    RankingUpdateMessage rankingMessage = (RankingUpdateMessage) message;
                    return rankingMessage.productId().equals(456L) &&
                            rankingMessage.actionType() == RankingActionType.LIKE_REMOVED &&
                            rankingMessage.eventId().equals("like-removed-001_RANKING");
                })
        );
    }

}