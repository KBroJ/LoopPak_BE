package com.loopers.consumer.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEventHandlerTest {

    @Mock
    private EventLogService eventLogService;

    @Mock
    private CacheEvictService cacheEvictService;

    @Mock
    private MetricsService metricsService;

    @Mock
    private EventDeserializer eventDeserializer;

    @Mock
    private ObjectMapper objectMapper;  // 추가된 Mock

    @InjectMocks
    private StockEventHandler stockEventHandler;

    @DisplayName("지원 이벤트 타입")
    @Nested
    class GetSupportedEventTypes {

        @Test
        @DisplayName("지원하는 이벤트 타입을 반환한다.")
        void returnsSupportedEventTypes() {
            // act
            String[] supportedTypes = stockEventHandler.getSupportedEventTypes();

            // assert
            assertThat(supportedTypes).containsExactly("StockDecreasedEvent", "StockIncreasedEvent");
        }
    }

    @DisplayName("재고 감소 이벤트 처리")
    @Nested
    class HandleStockDecreasedEvent {

        @Test
        @DisplayName("재고 감소 이벤트가 성공적으로 처리된다.")
        void handle_ValidStockDecreasedEvent_ProcessesSuccessfully() throws Exception {
            // arrange
            String eventType = "StockDecreasedEvent";
            String payloadJson = "{\"productId\":456,\"currentStock\":5,\"previousStock\":10}";
            String messageKey = "456";
            Object mockStockEvent = new Object();

            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockCurrentStockNode = mock(JsonNode.class);

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenReturn(mockJsonNode);
            when(mockJsonNode.get("currentStock")).thenReturn(mockCurrentStockNode);
            when(mockCurrentStockNode.asInt()).thenReturn(5);

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockStockEvent, "StockDecreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(metricsService).increaseSalesCount(456L);
            verify(objectMapper).readTree(payloadJson);
        }

        @Test
        @DisplayName("재고 소진 시 상품 목록 캐시가 추가로 무효화된다.")
        void handle_StockDecreasedToZero_EvictsProductListCache() throws Exception {
            // arrange
            String eventType = "StockDecreasedEvent";
            String payloadJson = "{\"productId\":456,\"currentStock\":0,\"previousStock\":1}";
            String messageKey = "456";
            Object mockStockEvent = new Object();

            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockCurrentStockNode = mock(JsonNode.class);

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenReturn(mockJsonNode);
            when(mockJsonNode.get("currentStock")).thenReturn(mockCurrentStockNode);
            when(mockCurrentStockNode.asInt()).thenReturn(0); // 재고 소진

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockStockEvent, "StockDecreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictProductListCache(); // 추가 캐시 무효화
            verify(metricsService).increaseSalesCount(456L);
        }

        @Test
        @DisplayName("재고 감소 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_StockDecreasedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "StockDecreasedEvent";
            String payloadJson = "{\"productId\":456}";
            String messageKey = "456";

            when(eventDeserializer.deserialize(any(), any()))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> stockEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("역직렬화 실패");
        }
    }

    @DisplayName("재고 증가 이벤트 처리")
    @Nested
    class HandleStockIncreasedEvent {

        @Test
        @DisplayName("재고 증가 이벤트가 성공적으로 처리된다.")
        void handle_ValidStockIncreasedEvent_ProcessesSuccessfully() throws Exception {
            // arrange
            String eventType = "StockIncreasedEvent";
            String payloadJson = "{\"productId\":456,\"currentStock\":10,\"previousStock\":5}";
            String messageKey = "456";
            Object mockStockEvent = new Object();

            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockPreviousStockNode = mock(JsonNode.class);
            JsonNode mockCurrentStockNode = mock(JsonNode.class);

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenReturn(mockJsonNode);
            when(mockJsonNode.get("previousStock")).thenReturn(mockPreviousStockNode);
            when(mockJsonNode.get("currentStock")).thenReturn(mockCurrentStockNode);
            when(mockPreviousStockNode.asInt()).thenReturn(5);
            when(mockCurrentStockNode.asInt()).thenReturn(10);

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockStockEvent, "StockIncreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(objectMapper).readTree(payloadJson);
        }

        @Test
        @DisplayName("품절 복구 시 상품 목록 캐시가 추가로 무효화된다.")
        void handle_StockIncreasedFromZero_EvictsProductListCache() throws Exception {
            // arrange
            String eventType = "StockIncreasedEvent";
            String payloadJson = "{\"productId\":456,\"currentStock\":5,\"previousStock\":0}";
            String messageKey = "456";
            Object mockStockEvent = new Object();

            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockPreviousStockNode = mock(JsonNode.class);
            JsonNode mockCurrentStockNode = mock(JsonNode.class);

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(objectMapper.readTree(payloadJson))
                    .thenReturn(mockJsonNode);
            when(mockJsonNode.get("previousStock")).thenReturn(mockPreviousStockNode);
            when(mockJsonNode.get("currentStock")).thenReturn(mockCurrentStockNode);
            when(mockPreviousStockNode.asInt()).thenReturn(0); // 품절 상태였음
            when(mockCurrentStockNode.asInt()).thenReturn(5);  // 재입고됨

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockStockEvent, "StockIncreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictProductListCache(); // 품절 복구로 인한 추가 캐시 무효화
        }

        @Test
        @DisplayName("재고 증가 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_StockIncreasedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "StockIncreasedEvent";
            String payloadJson = "{\"productId\":456}";
            String messageKey = "456";

            when(eventDeserializer.deserialize(any(), any()))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> stockEventHandler.handle(eventType, payloadJson, messageKey))
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
            assertThatThrownBy(() -> stockEventHandler.handle(eventType, payloadJson, messageKey))
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
            String eventType = "StockDecreasedEvent";
            String payloadJson = "{\"productId\":456}";
            String messageKey = "invalid";

            // act & assert
            assertThatThrownBy(() -> stockEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(NumberFormatException.class);
        }
    }
}