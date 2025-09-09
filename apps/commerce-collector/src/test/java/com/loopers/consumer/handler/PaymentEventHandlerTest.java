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
    private EventHandledService eventHandledService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentEventHandler paymentEventHandler;

    @BeforeEach
    void setUp() {
        paymentEventHandler = new PaymentEventHandler(
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

        @DisplayName("PaymentSuccessEvent와 PaymentFailureEvent 지원 여부를 올바르게 반환한다")
        @Test
        void getSupportedEventTypes_ReturnsCorrectTypes() {
            // act
            String[] supportedTypes = paymentEventHandler.getSupportedEventTypes();

            // assert
            assertThat(supportedTypes).containsExactly("PaymentSuccessEvent", "PaymentFailureEvent");
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
                      "eventId": "payment-success-001",
                      "orderId": 123,
                      "userId": 456,
                      "paymentType": "CARD",
                      "transactionKey": "tx_123456",
                      "amount": 50000
                  }
                  """;

            Object mockPaymentEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockPaymentEvent);
            when(eventHandledService.isAlreadyHandled("payment-success-001"))
                    .thenReturn(false);

            // act
            paymentEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 핵심 비즈니스 로직 검증
            verify(eventDeserializer).deserialize(payloadJson, Object.class);
            verify(eventLogService).saveEventLog(
                    eq(mockPaymentEvent),
                    eq("PaymentSuccessEvent"),
                    eq("123"),
                    eq("ORDER")
            );

            // 멱등성 관련 검증
            verify(eventHandledService).isAlreadyHandled("payment-success-001");
            verify(eventHandledService).markAsHandled("payment-success-001", "PaymentSuccessEvent", "123");
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
                      "eventId": "payment-failure-001",
                      "orderId": 123,
                      "userId": 456,
                      "paymentType": "CARD",
                      "amount": 50000,
                      "message": "카드 승인 실패"
                  }
                  """;

            Object mockPaymentEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockPaymentEvent);
            when(eventHandledService.isAlreadyHandled("payment-failure-001"))
                    .thenReturn(false);

            // act
            paymentEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(
                    eq(mockPaymentEvent),
                    eq("PaymentFailureEvent"),
                    eq("123"),
                    eq("ORDER")
            );

            // 멱등성 관련 검증
            verify(eventHandledService).isAlreadyHandled("payment-failure-001");
            verify(eventHandledService).markAsHandled("payment-failure-001", "PaymentFailureEvent", "123");
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
            String payloadJson = """
                  {
                      "eventId": "payment-invalid-001"
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("payment-invalid-001"))
                    .thenReturn(false);

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
            String payloadJson = """
                  {
                      "eventId": "unsupported-001"
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("unsupported-001"))
                    .thenReturn(false);

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
            String payloadJson = """
                  {
                      "eventId": "payment-deserialize-fail-001"
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("payment-deserialize-fail-001"))
                    .thenReturn(false);
            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenThrow(new RuntimeException("Deserialization failed"));

            // act & assert
            assertThatThrownBy(() ->
                    paymentEventHandler.handle(eventType, payloadJson, messageKey)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Deserialization failed");

            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }

        @DisplayName("중복 결제 성공 이벤트는 처리하지 않는다")
        @Test
        void handle_DuplicatePaymentSuccessEvent_SkipsProcessing() {
            // arrange
            String eventType = "PaymentSuccessEvent";
            String messageKey = "123";
            String payloadJson = """
                  {
                      "eventId": "payment-duplicate-001",
                      "orderId": 123
                  }
                  """;

            when(eventHandledService.isAlreadyHandled("payment-duplicate-001"))
                    .thenReturn(true); // 이미 처리됨

            // act
            paymentEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 비즈니스 로직 호출 안됨
            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
            verify(eventDeserializer, never()).deserialize(any(), any());
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }

        @DisplayName("eventId가 없으면 예외가 발생한다")
        @Test
        void handle_MissingEventId_ThrowsException() {
            // arrange
            String eventType = "PaymentSuccessEvent";
            String messageKey = "123";
            String payloadJson = "{}"; // eventId 없음

            // act & assert
            assertThatThrownBy(() ->
                    paymentEventHandler.handle(eventType, payloadJson, messageKey)
            ).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("eventId 추출 실패");
        }
    }
}