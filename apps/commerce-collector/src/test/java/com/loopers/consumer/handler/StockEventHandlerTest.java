package com.loopers.consumer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.cache.CacheEvictService;
import com.loopers.application.eventhandled.EventHandledService;
import com.loopers.application.eventhandler.StockEventHandler;
import com.loopers.application.eventlog.EventLogService;
import com.loopers.application.metrics.MetricsService;
import com.loopers.common.EventDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private EventHandledService eventHandledService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StockEventHandler stockEventHandler;

    @BeforeEach
    void setUp() {
        stockEventHandler = new StockEventHandler(
                eventLogService,
                cacheEvictService,
                metricsService,
                eventDeserializer,
                objectMapper,
                eventHandledService
        );
    }

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
            String payloadJson = """
                  {
                    "eventId": "stock-decreased-001",
                    "productId": 456,
                    "currentStock": 5,
                    "previousStock": 10
                  }
                  """;
            String messageKey = "456";
            Object mockStockEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(eventHandledService.isAlreadyHandled("stock-decreased-001"))
                    .thenReturn(false);

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 핵심 비즈니스 로직 검증
            verify(eventLogService).saveEventLog(mockStockEvent, "StockDecreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(metricsService).increaseSalesCount(456L);

            // 멱등성 관련 검증
            verify(eventHandledService).isAlreadyHandled("stock-decreased-001");
            verify(eventHandledService).markAsHandled("stock-decreased-001", "StockDecreasedEvent", "456");
        }

        @Test
        @DisplayName("재고 소진 시 상품 목록 캐시가 추가로 무효화된다.")
        void handle_StockDecreasedToZero_EvictsProductListCache() throws Exception {
            // arrange
            String eventType = "StockDecreasedEvent";
            String payloadJson = """
                  {
                    "eventId": "stock-decreased-002",
                    "productId": 456,
                    "currentStock": 0,
                    "previousStock": 1
                  }
                  """;
            String messageKey = "456";
            Object mockStockEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(eventHandledService.isAlreadyHandled("stock-decreased-002"))
                    .thenReturn(false);

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockStockEvent, "StockDecreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictProductListCache(); // 추가 캐시 무효화
            verify(metricsService).increaseSalesCount(456L);
            verify(eventHandledService).markAsHandled("stock-decreased-002", "StockDecreasedEvent", "456");
        }

        @Test
        @DisplayName("재고 감소 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_StockDecreasedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "StockDecreasedEvent";
            String payloadJson = """
                  {
                    "eventId": "stock-decreased-003",
                    "productId": 456
                  }
                  """;
            String messageKey = "456";

            when(eventHandledService.isAlreadyHandled("stock-decreased-003"))
                    .thenReturn(false);
            when(eventDeserializer.deserialize(any(), any()))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> stockEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("역직렬화 실패");

            // 예외 발생 시 완료 기록하지 않음
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
        }

        @Test
        @DisplayName("중복 재고 감소 이벤트는 처리하지 않는다.")
        void handle_DuplicateStockDecreasedEvent_SkipsProcessing() {
            // arrange
            String eventType = "StockDecreasedEvent";
            String payloadJson = """
                  {
                    "eventId": "stock-decreased-duplicate",
                    "productId": 456,
                    "currentStock": 5
                  }
                  """;
            String messageKey = "456";

            when(eventHandledService.isAlreadyHandled("stock-decreased-duplicate"))
                    .thenReturn(true); // 이미 처리됨

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert - 비즈니스 로직 호출 안됨
            verify(eventLogService, never()).saveEventLog(any(), any(), any(), any());
            verify(cacheEvictService, never()).evictProductCache(any());
            verify(metricsService, never()).increaseSalesCount(any());
            verify(eventHandledService, never()).markAsHandled(any(), any(), any());
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
            String payloadJson = """
                  {
                    "eventId": "stock-increased-001",
                    "productId": 456,
                    "currentStock": 10,
                    "previousStock": 5
                  }
                  """;
            String messageKey = "456";
            Object mockStockEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(eventHandledService.isAlreadyHandled("stock-increased-001"))
                    .thenReturn(false);

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockStockEvent, "StockIncreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(eventHandledService).markAsHandled("stock-increased-001", "StockIncreasedEvent", "456");
        }

        @Test
        @DisplayName("품절 복구 시 상품 목록 캐시가 추가로 무효화된다.")
        void handle_StockIncreasedFromZero_EvictsProductListCache() throws Exception {
            // arrange
            String eventType = "StockIncreasedEvent";
            String payloadJson = """
                  {
                    "eventId": "stock-increased-002",
                    "productId": 456,
                    "currentStock": 5,
                    "previousStock": 0
                  }
                  """;
            String messageKey = "456";
            Object mockStockEvent = new Object();

            when(eventDeserializer.deserialize(payloadJson, Object.class))
                    .thenReturn(mockStockEvent);
            when(eventHandledService.isAlreadyHandled("stock-increased-002"))
                    .thenReturn(false);

            // act
            stockEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(eventLogService).saveEventLog(mockStockEvent, "StockIncreasedEvent", "456", "PRODUCT");
            verify(cacheEvictService).evictProductCache(456L);
            verify(cacheEvictService).evictProductListCache(); // 품절 복구로 인한 추가 캐시 무효화
            verify(eventHandledService).markAsHandled("stock-increased-002", "StockIncreasedEvent", "456");
        }

        @Test
        @DisplayName("재고 증가 이벤트 처리 중 예외 발생 시 예외가 재발생한다.")
        void handle_StockIncreasedEventThrowsException_ReThrowsException() {
            // arrange
            String eventType = "StockIncreasedEvent";
            String payloadJson = """
                  {
                    "eventId": "stock-increased-003",
                    "productId": 456
                  }
                  """;
            String messageKey = "456";

            when(eventHandledService.isAlreadyHandled("stock-increased-003"))
                    .thenReturn(false);
            when(eventDeserializer.deserialize(any(), any()))
                    .thenThrow(new RuntimeException("역직렬화 실패"));

            // act & assert
            assertThatThrownBy(() -> stockEventHandler.handle(eventType, payloadJson, messageKey))
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
            String payloadJson = """
                  {
                    "eventId": "stock-invalid-001",
                    "productId": 456
                  }
                  """;
            String messageKey = "invalid";

            when(eventHandledService.isAlreadyHandled("stock-invalid-001"))
                    .thenReturn(false);

            // act & assert
            assertThatThrownBy(() -> stockEventHandler.handle(eventType, payloadJson, messageKey))
                    .isInstanceOf(NumberFormatException.class);
        }
    }
}