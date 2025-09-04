package com.loopers.consumer;

import com.loopers.collector.application.cache.CacheEvictService;
import com.loopers.collector.application.eventlog.EventLogService;
import com.loopers.collector.application.metrics.MetricsService;
import com.loopers.collector.common.EventDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * catalog-events 토픽의 이벤트를 소비하는 Consumer
 * 좋아요, 상품 조회 등 상품 카탈로그 관련 이벤트 처리
 *
 *  kafkaTemplate.send([토픽], [키], [메시지(@Payload)])
 *  @Payload - "실제 메시지 내용"
 *  @Header - "메시지 메타정보"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {

    private final EventLogService eventLogService;
    private final CacheEvictService cacheEvictService;
    private final MetricsService metricsService;
    private final EventDeserializer eventDeserializer;

    /**
     * catalog-events 토픽 메시지 처리
     *
     * PartitionKey로 productId를 사용하므로 같은 상품의 이벤트는 순서 보장됨
     */
    @KafkaListener(
            topics = "catalog-events",
            groupId = "commerce-collector",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCatalogEvent(
            @Payload String message,                                // 메시지 내용(@Payload : 실제 메시지 내용)
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,      // 토픽명
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, // 파티션 번호
            @Header(KafkaHeaders.OFFSET) long offset,               // 오프셋(메시지 순번)
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,   // 파티션 키(productId)
            Acknowledgment ack                                      // 수동 ACK : 메시지 처리 완료 확인용
    ) {
        log.info("Catalog 이벤트 수신 - topic: {}, partition: {}, offset: {}, key: {}", topic, partition, offset, messageKey);

        try {
            // 1. 이벤트 타입 감지
            String eventType = eventDeserializer.detectEventType(message);
            log.info("이벤트 타입 감지: {}", eventType);

            // 2. 이벤트 타입별 처리 분기
            switch (eventType) {
                case "LikeAddedEvent" -> handleLikeAddedEvent(message, messageKey);
                case "LikeRemovedEvent" -> handleLikeRemovedEvent(message, messageKey);
                default -> {
                    log.warn("처리할 수 없는 이벤트 타입 - eventType: {}, message: {}", eventType, message);
                    // 알 수 없는 이벤트도 감사 로그는 저장
                    eventLogService.saveEventLog(message, eventType, messageKey, "UNKNOWN");
                }
            }

            // 3. 메시지 처리 완료 확인 (Manual ACK)
            ack.acknowledge();
            log.info("Catalog 이벤트 처리 완료 - eventType: {}, key: {}", eventType, messageKey);

        } catch (Exception e) {
            log.error("Catalog 이벤트 처리 실패 - message: {}, error: {}", message, e.getMessage(), e);

            // TODO: DLQ(Dead Letter Queue) 처리 고려
            // 현재는 예외 발생 시 메시지 재시도됨 (Kafka Consumer 기본 동작)
            throw e;
        }
    }

    /**
     * 좋아요 추가 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + Metrics Update
     */
    private void handleLikeAddedEvent(String message, String productId) {
        try {
            // JSON을 LikeAddedEvent 객체로 변환 (가정)
            // 실제로는 LikeAddedEvent 클래스를 import 해야 함
            Object likeAddedEvent = eventDeserializer.deserialize(message, Object.class);

            Long productIdLong = Long.parseLong(productId);

            // 1. 감사 로그 저장
            eventLogService.saveEventLog(likeAddedEvent, "LikeAddedEvent", productId, "PRODUCT");

            // 2. 상품 관련 캐시 무효화
            cacheEvictService.evictProductCache(productIdLong);
            cacheEvictService.evictTopLikedProductsCache(); // 랭킹 캐시도 무효화

            // 3. 좋아요 수 증가 (집계)
            metricsService.increaseLikeCount(productIdLong);

            log.info("좋아요 추가 이벤트 처리 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("좋아요 추가 이벤트 처리 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 좋아요 제거 이벤트 처리
     * 3종 처리: Audit Log + Cache Evict + Metrics Update
     */
    private void handleLikeRemovedEvent(String message, String productId) {
        try {
            // JSON을 LikeRemovedEvent 객체로 변환 (가정)
            Object likeRemovedEvent = eventDeserializer.deserialize(message, Object.class);

            Long productIdLong = Long.parseLong(productId);

            // 1. 감사 로그 저장
            eventLogService.saveEventLog(likeRemovedEvent, "LikeRemovedEvent", productId, "PRODUCT");

            // 2. 상품 관련 캐시 무효화
            cacheEvictService.evictProductCache(productIdLong);
            cacheEvictService.evictTopLikedProductsCache(); // 랭킹 캐시도 무효화

            // 3. 좋아요 수 감소 (집계)
            metricsService.decreaseLikeCount(productIdLong);

            log.info("좋아요 제거 이벤트 처리 완료 - productId: {}", productId);

        } catch (Exception e) {
            log.error("좋아요 제거 이벤트 처리 실패 - productId: {}, error: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

}
