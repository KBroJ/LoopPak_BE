package com.loopers.application.eventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.eventhandled.EventHandledService;
import com.loopers.application.ranking.RankingActionType;
import com.loopers.application.ranking.RankingUpdateMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RankingEventHandler 단위테스트
 *
 * 검증 대상:
 * 1. 점수가 적절하게 반영된다 (좋아요 +0.2, 취소 -0.2)
 * 2. 날짜별 키 계산 기능 (ranking:all:yyyyMMdd)
 * 3. Redis ZSET 점수 반영 (incrementScore 호출)
 * 4. TTL 설정 (172800초)
 * 5. 멱등성 처리 (중복 이벤트 방지)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RankingEventHandler 단위테스트")
public class RankingEventHandlerTest {

    @InjectMocks
    private RankingEventHandler rankingEventHandler;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @Mock
    private EventHandledService eventHandledService;

    @Mock
    private ObjectMapper objectMapper;

    @DisplayName("랭킹 이벤트 처리")
    @Nested
    class HandleRankingEvent {

        @Test
        @DisplayName("좋아요 추가 이벤트 처리 시, Redis ZSET에 +0.2점이 반영된다.")
        void handleLikeAddedEvent_incrementsScoreByPointTwo() throws Exception {
            // arrange
            String eventId = "event-123";
            Long productId = 456L;
            String messageKey = "product-456";
            String eventType = "RankingUpdateMessage";
            String payloadJson = """
                  {
                      "eventId": "event-123",
                      "productId": 456,
                      "actionType": "LIKE_ADDED"
                  }
                  """;

            RankingUpdateMessage message = new RankingUpdateMessage(
                    productId, RankingActionType.LIKE_ADDED, eventId
            );

            String expectedKey = "ranking:all:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String expectedMember = "product:456";

            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(objectMapper.readValue(payloadJson, RankingUpdateMessage.class)).willReturn(message);
            given(eventHandledService.isAlreadyHandled(eventId)).willReturn(false);
            given(zSetOperations.incrementScore(expectedKey, expectedMember, 0.2)).willReturn(1.2);

            // act
            rankingEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(objectMapper).readValue(payloadJson, RankingUpdateMessage.class);
            verify(eventHandledService).isAlreadyHandled(eventId);
            verify(zSetOperations).incrementScore(expectedKey, expectedMember, 0.2);
            verify(redisTemplate).expire(expectedKey, 172800L, TimeUnit.SECONDS);
            verify(eventHandledService).markAsHandled(eventId, eventType, messageKey);
        }

        @Test
        @DisplayName("좋아요 취소 이벤트 처리 시, Redis ZSET에 -0.2점이 반영된다.")
        void handleLikeRemovedEvent_decrementsScoreByPointTwo() throws Exception {
            // arrange
            String eventId = "event-456";
            Long productId = 789L;
            String messageKey = "product-789";
            String eventType = "RankingUpdateMessage";
            String payloadJson = """
                  {
                      "eventId": "event-456",
                      "productId": 789,
                      "actionType": "LIKE_REMOVED"
                  }
                  """;

            RankingUpdateMessage message = new RankingUpdateMessage(
                    productId, RankingActionType.LIKE_REMOVED, eventId
            );

            String expectedKey = "ranking:all:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String expectedMember = "product:789";

            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(objectMapper.readValue(payloadJson, RankingUpdateMessage.class)).willReturn(message);
            given(eventHandledService.isAlreadyHandled(eventId)).willReturn(false);
            given(zSetOperations.incrementScore(expectedKey, expectedMember, -0.2)).willReturn(0.8);

            // act
            rankingEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(objectMapper).readValue(payloadJson, RankingUpdateMessage.class);
            verify(eventHandledService).isAlreadyHandled(eventId);
            verify(zSetOperations).incrementScore(expectedKey, expectedMember, -0.2);
            verify(redisTemplate).expire(expectedKey, 172800L, TimeUnit.SECONDS);
            verify(eventHandledService).markAsHandled(eventId, eventType, messageKey);
        }

        @Test
        @DisplayName("이미 처리된 이벤트는 중복 처리하지 않는다.")
        void doesNotProcessDuplicateEvent_whenEventAlreadyHandled() throws Exception {
            // arrange
            String eventId = "duplicate-event";
            Long productId = 123L;
            String messageKey = "product-123";
            String eventType = "RankingUpdateMessage";
            String payloadJson = """
                  {
                      "eventId": "duplicate-event",
                      "productId": 123,
                      "actionType": "LIKE_ADDED"
                  }
                  """;

            RankingUpdateMessage message = new RankingUpdateMessage(
                productId, RankingActionType.LIKE_ADDED, eventId
            );

            given(objectMapper.readValue(payloadJson, RankingUpdateMessage.class)).willReturn(message);
            given(eventHandledService.isAlreadyHandled(eventId)).willReturn(true);

            // act
            rankingEventHandler.handle(eventType, payloadJson, messageKey);

            // assert
            verify(zSetOperations, never()).incrementScore(anyString(), anyString(), anyDouble());
            verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
            verify(eventHandledService, never()).markAsHandled(anyString(), anyString(), anyString());
        }
    }

    @DisplayName("Redis 키 관리")
    @Nested
    class RedisKeyManagement {

        @Test
        @DisplayName("오늘 날짜 기반으로 ranking:all:yyyyMMdd 형식의 키를 생성한다.")
        void generatesCorrectRedisKey_basedOnTodayDate() throws Exception {
            // arrange
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String expectedKey = "ranking:all:" + today;

            String eventId = "key-test";
            Long productId = 999L;
            String payloadJson = """
                  {
                      "eventId": "key-test",
                      "productId": 999,
                      "actionType": "LIKE_ADDED"
                  }
                  """;

            RankingUpdateMessage message = new RankingUpdateMessage(
                productId, RankingActionType.LIKE_ADDED, eventId
            );

            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(objectMapper.readValue(payloadJson, RankingUpdateMessage.class)).willReturn(message);
            given(eventHandledService.isAlreadyHandled(eventId)).willReturn(false);

            // act
            rankingEventHandler.handle("RankingUpdateMessage", payloadJson, "test-key");

            // assert
            verify(zSetOperations).incrementScore(eq(expectedKey), anyString(), anyDouble());
            verify(redisTemplate).expire(eq(expectedKey), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("TTL을 2일(172800초)로 설정한다.")
        void setsTtlToTwoDays_whenUpdatingScore() throws Exception {
            // arrange
            String eventId = "ttl-test";
            Long productId = 777L;
            String payloadJson = """
                  {
                      "eventId": "ttl-test",
                      "productId": 777,
                      "actionType": "LIKE_ADDED"
                  }
                  """;

            RankingUpdateMessage message = new RankingUpdateMessage(
                productId, RankingActionType.LIKE_ADDED, eventId
            );

            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(objectMapper.readValue(payloadJson, RankingUpdateMessage.class)).willReturn(message);
            given(eventHandledService.isAlreadyHandled(eventId)).willReturn(false);

            // act
            rankingEventHandler.handle("RankingUpdateMessage", payloadJson, "test-key");

            // assert
            verify(redisTemplate).expire(anyString(), eq(172800L), eq(TimeUnit.SECONDS));
        }
    }

    @DisplayName("예외 처리")
    @Nested
    class ExceptionHandling {

        @Test
        @DisplayName("JSON 파싱 실패 시 RuntimeException을 발생시킨다.")
        void throwsRuntimeException_whenJsonParsingFails() throws Exception {
            // arrange
            String invalidJson = "invalid json";

            given(objectMapper.readValue(invalidJson, RankingUpdateMessage.class))
                    .willThrow(new RuntimeException("JSON 파싱 실패"));

            // act & assert
            assertThrows(RuntimeException.class, () -> {
                rankingEventHandler.handle("RankingUpdateMessage", invalidJson, "test-key");
            });

            verify(zSetOperations, never()).incrementScore(anyString(), anyString(), anyDouble());
        }
    }

    @Test
    @DisplayName("지원되는 이벤트 타입은 RankingUpdateMessage이다.")
    void supportedEventTypes_containsRankingUpdateMessage() {
        // act
        String[] supportedTypes = rankingEventHandler.getSupportedEventTypes();

        // assert
        assertThat(supportedTypes).hasSize(1);
        assertThat(supportedTypes[0]).isEqualTo("RankingUpdateMessage");
    }

}
