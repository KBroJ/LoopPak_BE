package com.loopers.infrastructure.pg;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PgClientConfig {
    @Bean
    public Request.Options pgRequestOptions() {
        return new Request.Options(
                3000,   // connectTimeout (3초)
                10000                   // readTimeout (10초)
        );
    }
}
