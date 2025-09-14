package com.loopers.common;

import com.fasterxml.jackson.databind.JsonNode;
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
     * 1. JSON의 eventType 필드에서 직접 읽기
     * 2. JSON에서 이벤트 클래스명을 추출하여 타입 결정
     *
     * @param json JSON 문자열
     * @return 이벤트 타입명 (예: "LikeAddedEvent", "StockDecreasedEvent")
     */
    public String detectEventType(String json) {
        try {

            // 1. JSON의 eventType 필드에서 직접 읽기
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode eventTypeNode = rootNode.get("eventType");

            if (eventTypeNode != null && !eventTypeNode.isNull()) {
                String eventType = eventTypeNode.asText();
                log.debug("이벤트 타입 감지 성공 (eventType 필드) - type: {}", eventType);
                return eventType;
            }

            // 2. JSON에서 이벤트 클래스명을 추출하여 타입 결정
            log.debug("eventType 필드 없음, 패턴 매칭으로 감지 시도");
            return detectEventTypeByPattern(json);

        } catch (Exception e) {
            log.error("이벤트 타입 감지 실패 - json: {}, error: {}", json, e.getMessage());
            return "UnknownEvent";
        }
    }

    /**
     * JSON에서 이벤트 클래스명을 추출하여 타입 결정
     */
    private String detectEventTypeByPattern(String json) {
        try {
            // Like 이벤트 감지
            if (json.contains("\"userId\"") && json.contains("\"targetId\"") && json.contains("\"likeType\"")) {
                if (json.contains("LikeAddedEvent") || (!json.contains("LikeRemovedEvent") && json.contains("LIKE")))
                {
                    return "LikeAddedEvent";
                } else {
                    return "LikeRemovedEvent";
                }
            }

            // Stock 이벤트 감지
            if (json.contains("\"productId\"") && json.contains("\"previousStock\"") &&
                    json.contains("\"currentStock\"")) {
                if (json.contains("\"decreasedQuantity\"") || json.contains("StockDecreasedEvent")) {
                    return "StockDecreasedEvent";
                } else if (json.contains("\"increasedQuantity\"") || json.contains("StockIncreasedEvent")) {
                    return "StockIncreasedEvent";
                }
            }

            log.warn("패턴 매칭으로도 이벤트 타입 감지 실패 - json: {}", json);
            return "UnknownEvent";

        } catch (Exception e) {
            log.error("패턴 매칭 감지 실패 - json: {}, error: {}", json, e.getMessage());
            return "UnknownEvent";
        }
    }

}
