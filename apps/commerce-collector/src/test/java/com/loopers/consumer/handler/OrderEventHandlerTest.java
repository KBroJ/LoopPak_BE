package com.loopers.consumer.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.collector.application.cache.CacheEvictService;
import com.loopers.collector.application.eventlog.EventLogService;
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
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderEventHandler orderEventHandler;

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
                      "orderId": 123,
                      "userId": 456,
                      "finalPrice": 50000,
                      "paymentType": "POINT"
                  }
                  """;

            Object mockOrderEvent = new Object();
            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockUserIdNode = mock(JsonNode.class);
            JsonNode mockFinalPriceNode = mock(JsonNode.class);
            JsonNode mockPaymentTypeNode = mock(JsonNode.class);

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockOrderEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenReturn(mockJsonNode);
            when(mockJsonNode.get("userId")).thenReturn(mockUserIdNode);
            when(mockUserIdNode.asLong()).thenReturn(456L);
            when(mockJsonNode.get("finalPrice")).thenReturn(mockFinalPriceNode);
            when(mockFinalPriceNode.asLong()).thenReturn(50000L);
            when(mockJsonNode.get("paymentType")).thenReturn(mockPaymentTypeNode);
            when(mockPaymentTypeNode.asText()).thenReturn("POINT");

            // act
            orderEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            assertAll(
                    () -> verify(eventDeserializer).deserialize(payloadJson, Object.class),
                    () -> verify(eventLogService).saveEventLog(
                            eq(mockOrderEvent),
                            eq("OrderCreatedEvent"),
                            eq("123"),
                            eq("ORDER")
                    ),
                    () -> verify(objectMapper).readTree(payloadJson)
            );
        }

        @DisplayName("잘못된 messageKey로 호출 시 NumberFormatException이 발생한다")
        @Test
        void handle_InvalidMessageKey_ThrowsNumberFormatException() {
            // arrange
            String eventType = "OrderCreatedEvent";
            String invalidMessageKey = "invalid-number";
            String payloadJson = "{}";

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
            String payloadJson = "{}";

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenThrow(new RuntimeException("Deserialization failed"));

            // act & assert
            assertThatThrownBy(() ->
                    orderEventHandler.handle(eventType, payloadJson, messageKey)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Deserialization failed");

            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
        }

        @DisplayName("JSON 파싱 실패해도 감사 로그는 저장되고 예외는 삼킨다")
        @Test
        void handle_JsonParsingFailure_ContinuesProcessing() throws Exception {
            // arrange
            String eventType = "OrderCreatedEvent";
            String messageKey = "123";
            String payloadJson = "{}";

            Object mockOrderEvent = new Object();
            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockOrderEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenThrow(new RuntimeException("JSON parsing failed"));

            // act
            orderEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            assertAll(
                    () -> verify(eventLogService).saveEventLog(
                            eq(mockOrderEvent),
                            eq("OrderCreatedEvent"),
                            eq("123"),
                            eq("ORDER")
                    ),
                    () -> verify(objectMapper).readTree(payloadJson)
            );
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
            String payloadJson = "{}";

            // act & assert
            assertThatThrownBy(() ->
                    orderEventHandler.handle(invalidEventType, payloadJson, messageKey)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지원하지 않는 이벤트 타입");
        }
    }
}