package com.loopers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 * RedisTemplate을 사용하여 Redis와 상호작용할 수 있도록 설정
 */
@Configuration
public class RedisConfig {

    // Java 객체를 자동으로 JSON으로 변환하여 Redis에 저장하고, 읽을 때는 JSON을 다시 Java 객체로 만들어줍니다.
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        // Value는 JSON 형태로 직렬화
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

}
