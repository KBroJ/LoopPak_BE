package com.loopers.config.feign;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.retry.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignResilienceConfig {

    @Bean
    public FeignDecorators feignDecorators(CircuitBreaker circuitBreaker, Retry retry) {
        return FeignDecorators.builder()
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .build();
    }
}