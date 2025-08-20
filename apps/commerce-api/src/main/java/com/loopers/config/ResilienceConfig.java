package com.loopers.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker pgCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)                              // 최근 10번의 호출을 기준으로 실패율 계산
                .failureRateThreshold(50)                           // 50% 이상 실패하면 Circuit 열림
                .waitDurationInOpenState(Duration.ofSeconds(10))    // 10초 후 Half-Open 상태로 전환
                .permittedNumberOfCallsInHalfOpenState(3)           // Half-Open에서 3번 테스트 호출
                .slowCallDurationThreshold(Duration.ofSeconds(3))   // 3초 이상 걸리면 느린 호출로 간주
                .slowCallRateThreshold(50)                          // 50% 이상이 느린 호출이면 Circuit 열림
                .build();

        return CircuitBreaker.of("pgCircuitBreaker", config);
    }

    @Bean
    public Retry pgRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                                                         // 최대 3번 재시도
                .waitDuration(Duration.ofSeconds(1))                                    // 재시도 간격 1초
                .retryOnException(throwable ->
                        throwable instanceof feign.RetryableException ||                // Feign 재시도 가능한 예외
                                throwable instanceof java.net.SocketTimeoutException    // 타임아웃
                )
                .build();

        return Retry.of("pgRetry", config);
    }

}
