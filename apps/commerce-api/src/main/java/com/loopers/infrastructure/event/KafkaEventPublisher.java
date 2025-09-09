package com.loopers.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 발행을 위한 Publisher
 * EventEnvelope 패턴으로 표준화된 이벤트 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    /**
     * 이벤트를 Kafka로 발행
     *
     * 처리 과정:
     * 1. 이벤트를 EventEnvelope로 감싸기 (메타데이터 추가)
     * 2. JSON 직렬화하여 Kafka 전송
     * 3. PartitionKey 기반 순서 보장
     *
     * @param topic Kafka 토픽명 (예: "catalog-events")
     * @param partitionKey 파티션 키 (예: productId - 순서 보장용)
     * @param event 발행할 이벤트 객체
     */
    public void publish(String topic, String partitionKey, Object event) {

        try {

            // 이벤트를 EventEnvelope로 감싸기
            EventEnvelope envelope = EventEnvelope.wrap(event);

            // Kafka로 전송 (Spring이 자동으로 JSON 직렬화, At Least Once 보장을 위한 동기 전송)
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
