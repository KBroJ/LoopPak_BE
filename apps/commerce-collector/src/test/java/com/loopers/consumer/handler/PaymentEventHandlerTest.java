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
 * PaymentEventHandler 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventHandlerTest {

    @Mock
    private EventLogService eventLogService;

    @Mock
    private CacheEvictService cacheEvictService;

    @Mock
    private EventDeserializer eventDeserializer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentEventHandler paymentEventHandler;

    @DisplayName("이벤트 타입 지원 여부 확인")
    @Nested
    class EventTypeSupport {

        @DisplayName("PaymentSuccessEvent와 PaymentFailureEvent 지원 여부를 올바르게 반환한다")
        @Test
        void getSupportedEventTypes_ReturnsCorrectTypes() {
            // act
            String[] supportedTypes = paymentEventHandler.getSupportedEventTypes();

            // assert
            assertThat(supportedTypes).containsExactlyInAnyOrder("PaymentSuccessEvent", "PaymentFailureEvent");
        }

        @DisplayName("PaymentSuccessEvent는 처리 가능하다고 반환한다")
        @Test
        void canHandle_PaymentSuccessEvent_ReturnsTrue() {
            // arrange
            String eventType = "PaymentSuccessEvent";

            // act
            boolean canHandle = paymentEventHandler.canHandle(eventType);

            // assert
            assertThat(canHandle).isTrue();
        }

        @DisplayName("PaymentFailureEvent는 처리 가능하다고 반환한다")
        @Test
        void canHandle_PaymentFailureEvent_ReturnsTrue() {
            // arrange
            String eventType = "PaymentFailureEvent";

            // act
            boolean canHandle = paymentEventHandler.canHandle(eventType);

            // assert
            assertThat(canHandle).isTrue();
        }

        @DisplayName("지원하지 않는 이벤트 타입은 처리 불가능하다고 반환한다")
        @Test
        void canHandle_UnsupportedEvent_ReturnsFalse() {
            // arrange
            String eventType = "UnsupportedEvent";

            // act
            boolean canHandle = paymentEventHandler.canHandle(eventType);

            // assert
            assertThat(canHandle).isFalse();
        }
    }

    @DisplayName("PaymentSuccessEvent 처리")
    @Nested
    class HandlePaymentSuccessEvent {

        @DisplayName("정상적인 PaymentSuccessEvent를 성공적으로 처리한다")
        @Test
        void handle_ValidPaymentSuccessEvent_ProcessesSuccessfully() throws Exception {
            // arrange
            String eventType = "PaymentSuccessEvent";
            String messageKey = "123";
            String payloadJson = """
                  {
                      "orderId": 123,
                      "userId": 456,
                      "paymentType": "CARD",
                      "transactionKey": "tx_123456",
                      "amount": 50000
                  }
                  """;

            Object mockPaymentEvent = new Object();
            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockUserIdNode = mock(JsonNode.class);
            JsonNode mockPaymentTypeNode = mock(JsonNode.class);
            JsonNode mockAmountNode = mock(JsonNode.class);
            JsonNode mockTransactionKeyNode = mock(JsonNode.class);

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockPaymentEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenReturn(mockJsonNode);
            when(mockJsonNode.get("userId")).thenReturn(mockUserIdNode);
            when(mockUserIdNode.asLong()).thenReturn(456L);
            when(mockJsonNode.get("paymentType")).thenReturn(mockPaymentTypeNode);
            when(mockPaymentTypeNode.asText()).thenReturn("CARD");
            when(mockJsonNode.get("amount")).thenReturn(mockAmountNode);
            when(mockAmountNode.asLong()).thenReturn(50000L);
            when(mockJsonNode.get("transactionKey")).thenReturn(mockTransactionKeyNode);
            when(mockTransactionKeyNode.asText()).thenReturn("tx_123456");

            // act
            paymentEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            assertAll(
                    () -> verify(eventDeserializer).deserialize(payloadJson, Object.class),
                    () -> verify(eventLogService).saveEventLog(
                            eq(mockPaymentEvent),
                            eq("PaymentSuccessEvent"),
                            eq("123"),
                            eq("ORDER")
                    ),
                    () -> verify(objectMapper).readTree(payloadJson)
            );
        }
    }

    @DisplayName("PaymentFailureEvent 처리")
    @Nested
    class HandlePaymentFailureEvent {

        @DisplayName("정상적인 PaymentFailureEvent를 성공적으로 처리한다")
        @Test
        void handle_ValidPaymentFailureEvent_ProcessesSuccessfully() throws Exception {
            // arrange
            String eventType = "PaymentFailureEvent";
            String messageKey = "123";
            String payloadJson = """
                  {
                      "orderId": 123,
                      "userId": 456,
                      "paymentType": "CARD",
                      "amount": 50000,
                      "message": "카드 승인 실패"
                  }
                  """;

            Object mockPaymentEvent = new Object();
            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockUserIdNode = mock(JsonNode.class);
            JsonNode mockPaymentTypeNode = mock(JsonNode.class);
            JsonNode mockAmountNode = mock(JsonNode.class);
            JsonNode mockMessageNode = mock(JsonNode.class);

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockPaymentEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenReturn(mockJsonNode);
            when(mockJsonNode.get("userId")).thenReturn(mockUserIdNode);
            when(mockUserIdNode.asLong()).thenReturn(456L);
            when(mockJsonNode.get("paymentType")).thenReturn(mockPaymentTypeNode);
            when(mockPaymentTypeNode.asText()).thenReturn("CARD");
            when(mockJsonNode.get("amount")).thenReturn(mockAmountNode);
            when(mockAmountNode.asLong()).thenReturn(50000L);
            when(mockJsonNode.get("message")).thenReturn(mockMessageNode);
            when(mockMessageNode.asText()).thenReturn("카드 승인 실패");

            // act
            paymentEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            assertAll(
                    () -> verify(eventDeserializer).deserialize(payloadJson, Object.class),
                    () -> verify(eventLogService).saveEventLog(
                            eq(mockPaymentEvent),
                            eq("PaymentFailureEvent"),
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

        @DisplayName("잘못된 messageKey로 호출 시 NumberFormatException이 발생한다")
        @Test
        void handle_InvalidMessageKey_ThrowsNumberFormatException() {
            // arrange
            String eventType = "PaymentSuccessEvent";
            String invalidMessageKey = "invalid-number";
            String payloadJson = "{}";

            // act & assert
            assertThatThrownBy(() ->
                    paymentEventHandler.handle(eventType, payloadJson, invalidMessageKey)
            ).isInstanceOf(NumberFormatException.class);
        }

        @DisplayName("지원하지 않는 이벤트 타입으로 호출 시 IllegalArgumentException이 발생한다")
        @Test
        void handle_UnsupportedEventType_ThrowsIllegalArgumentException() {
            // arrange
            String invalidEventType = "UnsupportedEvent";
            String messageKey = "123";
            String payloadJson = "{}";

            // act & assert
            assertThatThrownBy(() ->
                    paymentEventHandler.handle(invalidEventType, payloadJson, messageKey)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지원하지 않는 이벤트 타입");
        }

        @DisplayName("EventDeserializer 실패 시 예외가 발생한다")
        @Test
        void handle_EventDeserializerFailure_ThrowsException() throws Exception {
            // arrange
            String eventType = "PaymentSuccessEvent";
            String messageKey = "123";
            String payloadJson = "{}";

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenThrow(new RuntimeException("Deserialization failed"));

            // act & assert
            assertThatThrownBy(() ->
                    paymentEventHandler.handle(eventType, payloadJson, messageKey)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Deserialization failed");

            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
        }

        @DisplayName("JSON 파싱 실패해도 감사 로그는 저장되고 예외는 삼킨다")
        @Test
        void handle_JsonParsingFailure_ContinuesProcessing() throws Exception {
            // arrange
            String eventType = "PaymentSuccessEvent";
            String messageKey = "123";
            String payloadJson = "{}";

            Object mockPaymentEvent = new Object();
            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockPaymentEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenThrow(new RuntimeException("JSON parsing failed"));

            // act
            paymentEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            assertAll(
                    () -> verify(eventLogService).saveEventLog(
                            eq(mockPaymentEvent),
                            eq("PaymentSuccessEvent"),
                            eq("123"),
                            eq("ORDER")
                    ),
                    () -> verify(objectMapper).readTree(payloadJson)
            );
        }
    }
}