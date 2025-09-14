package com.loopers.infrastructure.kafka;

import com.loopers.interfaces.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 발행을 위한 Publisher (commerce-collector용)
 *
 * 📋 역할:
 * - RankingUpdateMessage를 EventEnvelope로 감싸서 Kafka로 발행
 * - commerce-api의 KafkaEventPublisher와 동일한 기능
 *
 * 🔄 추후 개선사항:
 * - 과제 완료 후 modules/kafka로 공통화 예정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    /**
     * 이벤트를 Kafka로 발행
     *
     * @param topic Kafka 토픽명 (예: "ranking-events")
     * @param partitionKey 파티션 키 (예: productId - 순서 보장용)
     * @param event 발행할 이벤트 객체 (예: RankingUpdateMessage)
     */
    public void publish(String topic, String partitionKey, Object event) {
        try {
            // 이벤트를 EventEnvelope로 감싸기 (메타데이터 추가)
            EventEnvelope envelope = EventEnvelope.wrap(event);

            // Kafka로 전송 (동기식 - 성공/실패 즉시 확인)
            kafkaTemplate.send(topic, partitionKey, envelope).get();

            log.info("이벤트 발행 완료 - topic: {}, eventType: {}, eventId: {}, partitionKey: {}",
                    topic, envelope.eventType(), envelope.eventId(), partitionKey);

        } catch (Exception e) {
            log.error("이벤트 발행 실패 - topic: {}, partitionKey: {}, event: {}, error: {}",
                    topic, partitionKey, event.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Kafka 이벤트 발행 실패", e);
        }
    }

}
