package com.loopers.collector.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 이벤트 역직렬화 유틸리티
 * Kafka 메시지(JSON)를 이벤트 객체로 변환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventDeserializer {

    private final ObjectMapper objectMapper;

    /**
     * JSON 문자열을 특정 타입의 객체로 역직렬화
     *
     * @param json JSON 문자열
     * @param clazz 변환할 클래스 타입
     * @return 역직렬화된 객체
     */
    public <T> T deserialize(String json, Class<T> clazz) {
        try {
            log.debug("이벤트 역직렬화 시작 - targetClass: {}, json: {}", clazz.getSimpleName(), json);

            T result = objectMapper.readValue(json, clazz);

            log.debug("이벤트 역직렬화 완료 - targetClass: {}", clazz.getSimpleName());
            return result;

        } catch (Exception e) {
            log.error("이벤트 역직렬화 실패 - targetClass: {}, json: {}, error: {}",
                    clazz.getSimpleName(), json, e.getMessage(), e);
            throw new RuntimeException("이벤트 역직렬화 실패: " + clazz.getSimpleName(), e);
        }
    }

    /**
     * 이벤트 타입 감지
     * JSON에서 이벤트 클래스명을 추출하여 타입 결정
     *
     * @param json JSON 문자열
     * @return 이벤트 타입명 (예: "LikeAddedEvent")
     */
    public String detectEventType(String json) {
        try {
            // 간단한 패턴 매칭으로 이벤트 타입 감지
            if (json.contains("\"userId\"") && json.contains("\"targetId\"") && json.contains("\"likeType\"")) {
                if (json.contains("LikeAddedEvent") || (!json.contains("LikeRemovedEvent") && json.contains("LIKE"))) {
                    return "LikeAddedEvent";
                } else {
                    return "LikeRemovedEvent";
                }
            }

            // 다른 이벤트 타입들 추가 가능
            log.warn("알 수 없는 이벤트 타입 - json: {}", json);
            return "UnknownEvent";

        } catch (Exception e) {
            log.error("이벤트 타입 감지 실패 - json: {}, error: {}", json, e.getMessage());
            return "UnknownEvent";
        }
    }

}
