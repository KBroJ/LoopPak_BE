package com.loopers.consumer.handler;

import com.loopers.collector.application.cache.CacheEvictService;
import com.loopers.collector.application.eventlog.EventLogService;
import com.loopers.collector.application.metrics.MetricsService;
import com.loopers.collector.common.EventDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeEventHandlerTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private EventLogService eventLogService;

    @Mock
    private CacheEvictService cacheEvictService;

    @Mock
    private EventDeserializer eventDeserializer;

    @InjectMocks
    private LikeEventHandler likeEventHandler;

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
        void handle_ValidLikeAddedEvent_ProcessesSuccessfully() {
            // arrange
            String eventType = "LikeAddedEvent";
            String payloadJson = "{\"userId\":123,\"productId\":456}";
            String messageKey = "456";
            Object mockLikeEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockLikeEvent);

            // act
            likeEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockLikeEvent, "LikeAddedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictTopLikedProductsCache();
            verify(metricsService).increaseLikeCount(456L);
        }

        @Test
        @DisplayName("좋아요 추가 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_LikeAddedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "LikeAddedEvent";
            String payloadJson = "{\"userId\":123,\"productId\":456}";
            String messageKey = "456";

            when(eventDeserializer.deserialize(any(), any()))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> likeEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("역직렬화 실패");
        }
    }

    @DisplayName("좋아요 취소 이벤트 처리")
    @Nested
    class HandleLikeRemovedEvent {

        @Test
        @DisplayName("좋아요 취소 이벤트가 성공적으로 처리된다.")
        void handle_ValidLikeRemovedEvent_ProcessesSuccessfully() {
            // arrange
            String eventType = "LikeRemovedEvent";
            String payloadJson = "{\"userId\":123,\"productId\":456}";
            String messageKey = "456";
            Object mockLikeEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockLikeEvent);

            // act
            likeEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockLikeEvent, "LikeRemovedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictTopLikedProductsCache();
            verify(metricsService).decreaseLikeCount(456L);
        }

        @Test
        @DisplayName("좋아요 취소 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_LikeRemovedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "LikeRemovedEvent";
            String payloadJson = "{\"userId\":123,\"productId\":456}";
            String messageKey = "456";

            when(eventDeserializer.deserialize(any(), any()))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> likeEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("역직렬화 실패");
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
            String payloadJson = "{\"data\":\"test\"}";
            String messageKey = "123";

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
            String payloadJson = "{\"userId\":123,\"productId\":456}";
            String messageKey = "invalid";

            // act & assert
            assertThatThrownBy(() -> likeEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

}