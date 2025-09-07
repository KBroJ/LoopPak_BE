package com.loopers.consumer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.collector.application.cache.CacheEvictService;
import com.loopers.collector.application.eventhandled.EventHandledService;
import com.loopers.collector.application.eventlog.EventLogService;
import com.loopers.collector.common.EventDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderEventHandler 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @Mock
    private EventLogService eventLogService;
    @Mock
    private CacheEvictService cacheEvictService;
    @Mock
    private EventDeserializer eventDeserializer;
    @Mock
    private EventHandledService eventHandledService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrderEventHandler orderEventHandler;

    @BeforeEach
    void setUp() {
        orderEventHandler = new OrderEventHandler(
                eventLogService,
                cacheEvictService,
                eventDeserializer,
                objectMapper,
                eventHandledService
        );
    }

    @DisplayName("이벤트 타입 지원 여부 확인")
    @Nested
    class EventTypeSupport {

        @DisplayName("OrderCreatedEvent 지원 여부를 올바르게 반환한다")
        @Test
        void getSupportedEventTypes_ReturnsCorrectTypes() {
            // act
            String[] supportedTypes = orderEventHandler.getSupportedEventTypes();

            // assert
            assertThat(supportedTypes).containsExactly("OrderCreatedEvent");
        }

        @DisplayName("OrderCreatedEvent는 처리 가능하다고 반환한다")
        @Test
        void canHandle_OrderCreatedEvent_ReturnsTrue() {
            // arrange
            String eventType = "OrderCreatedEvent";

            // act
            boolean canHandle = orderEventHandler.canHandle(eventType);

            // assert
            assertThat(canHandle).isTrue();
        }

        @DisplayName("지원하지 않는 이벤트 타입은 처리 불가능하다고 반환한다")
        @Test
        void canHandle_UnsupportedEvent_ReturnsFalse() {
            // arrange
            String eventType = "UnsupportedEvent";

            // act
            boolean canHandle = orderEventHandler.canHandle(eventType);

            // assert
            assertThat(canHandle).isFalse();
        }
    }

    @DisplayName("OrderCreatedEvent 처리")
    @Nested
    class HandleOrderCreatedEvent {

        @DisplayName("정상적인 OrderCreatedEvent를 성공적으로 처리한다")
        @Test
        void handle_ValidOrderCreatedEvent_ProcessesSuccessfully() throws Exception {
            // arrange
            String eventType = "OrderCreatedEvent";
            String messageKey = "123";
            String payloadJson = """
                  {
                      "eventId": "order-created-001",
                      "orderId": 123,
                      "userId": 456,
                      "finalPrice": 50000,
                      "paymentType": "POINT"
                  }
                  """;

            Object mockOrderEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockOrderEvent);
            when(eventHandledService.isAlreadyHandled("order-created-001"))
                    .thenReturn(false);

            // act
            orderEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 핵심 비즈니스 로직 검증
            verify(eventDeserializer).deserialize(payloadJson, Object.class);
            verify(eventLogService).saveEventLog(
                    eq(mockOrderEvent),
                    eq("OrderCreatedEvent"),
                    eq("123"),
                    eq("ORDER")
            );

            // 멱등성 관련 검증
            verify(eventHandledService).isAlreadyHandled("order-created-001");
            verify(eventHandledService).markAsHandled("order-created-001", "OrderCreatedEvent", "123");
        }

        @DisplayName("잘못된 messageKey로 호출 시 NumberFormatException이 발생한다")
        @Test
        void handle_InvalidMessageKey_ThrowsNumberFormatException() {
            // arrange
            String eventType = "OrderCreatedEvent";
            String invalidMessageKey = "invalid-number";
            String payloadJson = """
                  {
                      "eventId": "order-invalid-001"
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("order-invalid-001"))
                    .thenReturn(false);

            // act & assert
            assertThatThrownBy(() ->
                    orderEventHandler.handle(eventType, payloadJson, invalidMessageKey)
            ).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("EventDeserializer 실패 시 예외가 발생한다")
        @Test
        void handle_EventDeserializerFailure_ThrowsException() throws Exception {
            // arrange
            String eventType = "OrderCreatedEvent";
            String messageKey = "123";
            String payloadJson = """
                  {
                      "eventId": "order-deserialize-fail-001"
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("order-deserialize-fail-001"))
                    .thenReturn(false);
            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenThrow(new RuntimeException("Deserialization failed"));

            // act & assert
            assertThatThrownBy(() ->
                    orderEventHandler.handle(eventType, payloadJson, messageKey)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Deserialization failed");

            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }

        @DisplayName("중복 주문 생성 이벤트는 처리하지 않는다")
        @Test
        void handle_DuplicateOrderCreatedEvent_SkipsProcessing() {
            // arrange
            String eventType = "OrderCreatedEvent";
            String messageKey = "123";
            String payloadJson = """
                  {
                      "eventId": "order-duplicate-001",
                      "orderId": 123
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("order-duplicate-001"))
                    .thenReturn(true); // 이미 처리됨

            // act
            orderEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 비즈니스 로직 호출 안됨
            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
            verify(eventDeserializer, never()).deserialize(any(), any());
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }
    }

    @DisplayName("예외 상황 처리")
    @Nested
    class ExceptionHandling {

        @DisplayName("지원하지 않는 이벤트 타입으로 호출 시 IllegalArgumentException이 발생한다")
        @Test
        void handle_UnsupportedEventType_ThrowsIllegalArgumentException() {
            // arrange
            String invalidEventType = "UnsupportedEvent";
            String messageKey = "123";
            String payloadJson = """
                  {
                      "eventId": "unsupported-001"
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("unsupported-001"))
                    .thenReturn(false);

            // act & assert
            assertThatThrownBy(() ->
                    orderEventHandler.handle(invalidEventType, payloadJson, messageKey)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지원하지 않는 이벤트 타입");
        }

        @DisplayName("eventId가 없으면 예외가 발생한다")
        @Test
        void handle_MissingEventId_ThrowsException() {
            // arrange
            String eventType = "OrderCreatedEvent";
            String messageKey = "123";
            String payloadJson = "{}"; // eventId 없음

            // act & assert
            assertThatThrownBy(() ->
                    orderEventHandler.handle(eventType, payloadJson, messageKey)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("eventId 추출 실패");
        }
    }
}