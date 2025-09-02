package com.loopers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CommerceCollectorApplicationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Test
    void contextLoads() {
        // Spring Boot 애플리케이션이 정상 구동되는지 확인
    }

    @Test
    void kafkaTemplateIsInjected() {
        // KafkaTemplate이 정상 주입되는지 확인
        assertNotNull(kafkaTemplate);
    }

    @Test
    void testKafkaConnection() throws Exception {

        // arrange : 테스트용 토픽과 메시지 설정
        String topic = "test-topic";
        String message = "Hello Commerce Collector!";

        // act : Kafka에 메시지 전송 (비동기)
        var result = kafkaTemplate.send(topic, message)
                .get(5, TimeUnit.SECONDS); // 결과를 최대 5초 대기(성공하면 SendResult 객체 반환, 실패하면 Exception 발생)
        var metadata = result.getRecordMetadata();


        // assert : 전송 결과 확인
        /*
           getRecordMetadata() : 전송된 결과 정보(메타데이터(토픽, 파티션, 오프셋 등))를 포함하는 객체
               RecordMetadata: Kafka가 반환한 전송 결과 정보
                  - 포함 정보:
                    - topic() - 실제로 전송된 토픽명
                    - partition() - 저장된 파티션 번호
                    - offset() - 저장된 오프셋 위치
         */
        assertNotNull(result, "전송 결과가 null이면 안됩니다");
        assertNotNull(metadata, "메타데이터가 null이면 안됩니다");
        assertEquals(topic, metadata.topic(), "전송된 토픽이 예상과 다릅니다");
        assertTrue(metadata.partition() >= 0, "파티션 번호는 0 이상이어야 합니다");
        assertTrue(metadata.offset() >= 0, "오프셋은 0 이상이어야 합니다");

        System.out.println("Kafka 연결 성공 - 토픽: " + metadata.topic() +
                                        ", 파티션: " + metadata.partition() +
                                        ", 오프셋: " + metadata.offset());
    }

}
