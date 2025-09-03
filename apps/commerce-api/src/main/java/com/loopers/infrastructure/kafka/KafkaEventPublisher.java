package com.loopers.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, Object event) {

        try {

            // At Least Once 보장을 위한 동기 전송
            kafkaTemplate.send(topic, key, event).get();
            log.info("Kafka 이벤트 발행 성공 - topic: {}, key: {}, event: {}", topic, key, event.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Kafka 이벤트 발행 실패 - topic: {}, key: {}, error: {}", topic, key, e.getMessage(), e);
            throw new RuntimeException("Kafka 이벤트 발행 실패", e);
        }

    }

}
