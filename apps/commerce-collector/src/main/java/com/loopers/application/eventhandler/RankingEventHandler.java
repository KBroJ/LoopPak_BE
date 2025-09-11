package com.loopers.application.eventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.eventhandled.EventHandledService;
import com.loopers.application.ranking.RankingUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 랭킹 업데이트 이벤트 처리 Handler
 *
 * 담당 이벤트:
 * - RankingUpdateMessage: 좋아요/주문 등에 따른 랭킹 점수 변경
 *
 * 처리 로직:
 * 1. Audit Log: 모든 랭킹 이벤트를 event_handled 테이블에 기록 (멱등성)
 * 2. Redis ZSET 업데이트: 일별 키에 상품 점수 증감 반영
 * 3. TTL 설정: 2일 후 자동 만료
 *
 * Redis ZSET 전략:
 * - Key: ranking:all:{yyyyMMdd} (예: ranking:all:20250111)
 * - Member: product:{productId} (예: product:123)
 * - Score: 누적 점수 (좋아요 +0.2, 취소 -0.2)
 * - TTL: 2일 (172800초)
 *
 * 왜 일별 키를 사용하나요?
 * - 시간의 양자화: 오늘과 어제 랭킹 분리
 * - 콜드 스타트 방지: 전날 점수 일부 승계 가능
 * - 메모리 관리: TTL로 자동 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingEventHandler implements EventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EventHandledService eventHandledService;
    private final ObjectMapper objectMapper;

    // Redis 키 관련 상수
    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final String PRODUCT_MEMBER_PREFIX = "product:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long TTL_DAYS = 2L;
    private static final long TTL_SECONDS = TTL_DAYS * 24 * 60 * 60; // 2일 = 172800초

    /**
     * 처리 가능한 이벤트 타입들 반환
     */
    @Override
    public String[] getSupportedEventTypes() {
        return new String[]{"RankingUpdateMessage"};
    }

    /**
     * 랭킹 업데이트 이벤트 처리 메인 로직
     *
     * 처리 흐름:
     * 1. JSON → RankingUpdateMessage 파싱
     * 2. 멱등성 체크 (eventId 기반)
     * 3. Redis ZSET 점수 증감 (ZINCRBY)
     * 4. TTL 설정 (KEY 생존 시간)
     * 5. 처리 완료 기록
     */
    @Override
    public void handle(String eventType, String payloadJson, String messageKey) {
        try {
            // 1. JSON → RankingUpdateMessage 파싱
            RankingUpdateMessage rankingMessage;
            try {
                rankingMessage = objectMapper.readValue(payloadJson, RankingUpdateMessage.class);
            } catch (Exception parseException) {
                log.error("RankingUpdateMessage 파싱 실패 - payloadJson: {}, error: {}",
                        payloadJson, parseException.getMessage());
                throw new RuntimeException("RankingUpdateMessage 파싱 실패", parseException);
            }

            String eventId = rankingMessage.eventId();
            Long productId = rankingMessage.productId();
            double scoreChange = rankingMessage.actionType().getScoreChange();

            // 2. 멱등성 체크 (이미 처리된 이벤트인지 확인)
            if (eventHandledService.isAlreadyHandled(eventId)) {
                log.info("이미 처리된 랭킹 이벤트 - eventId: {}, productId: {}", eventId, productId);
                return; // 중복 이벤트면 바로 종료
            }

            // 3. Redis ZSET 업데이트 (핵심 로직)
            updateRankingScore(productId, scoreChange);

            // 4. 처리 완료 기록 (멱등성 보장)
            eventHandledService.markAsHandled(eventId, eventType, messageKey);

            log.info("랭킹 업데이트 완료 - eventId: {}, productId: {}, scoreChange: {}",
                    eventId, productId, scoreChange);

        } catch (Exception e) {
            log.error("랭킹 이벤트 처리 실패 - eventType: {}, messageKey: {}, payloadJson: {}, error: {}",
                    eventType, messageKey, payloadJson, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Redis ZSET에 상품 랭킹 점수 업데이트
     *
     * Redis 연산:
     * 1. ZINCRBY: 기존 점수에 scoreChange 만큼 증감
     * 2. EXPIRE: TTL 2일 설정 (키가 없었다면 새로 생성 후 설정)
     *
     * @param productId 상품 ID
     * @param scoreChange 점수 변화량 (+0.2, -0.2 등)
     */
    private void updateRankingScore(Long productId, double scoreChange) {
        try {
            // 1. Redis 키 생성 (오늘 날짜 기반)
            String todayKey = generateTodayRankingKey();
            String productMember = PRODUCT_MEMBER_PREFIX + productId;

            log.debug("Redis ZSET 업데이트 시작 - key: {}, member: {}, scoreChange: {}",
                    todayKey, productMember, scoreChange);

            // 2. ZINCRBY: 점수 증감 (기존 점수가 없으면 0에서 시작)
            Double newScore = redisTemplate.opsForZSet().incrementScore(todayKey, productMember, scoreChange);

            // 3. TTL 설정 (키가 새로 생성되었을 수 있으므로 매번 설정)
            redisTemplate.expire(todayKey, TTL_SECONDS, TimeUnit.SECONDS);

            log.debug("Redis ZSET 업데이트 완료 - key: {}, member: {}, newScore: {}, TTL: {}일",
                    todayKey, productMember, newScore, TTL_DAYS);

        } catch (Exception e) {
            log.error("Redis ZSET 업데이트 실패 - productId: {}, scoreChange: {}, error: {}",
                    productId, scoreChange, e.getMessage(), e);
            throw new RuntimeException("랭킹 점수 업데이트 실패", e);
        }
    }

    /**
     * 오늘 날짜 기반 랭킹 키 생성
     *
     * 키 형식: ranking:all:{yyyyMMdd}
     * 예시: ranking:all:20250111
     *
     * @return 오늘의 랭킹 키
     */
    private String generateTodayRankingKey() {
        String today = LocalDate.now().format(DATE_FORMAT);
        return RANKING_KEY_PREFIX + today;
    }

}
