package com.loopers.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonFeignClientConfig {

    @Bean
    public RequestInterceptor commonHeaderInterceptor() {
        return new CommonHeaderInterceptor();
    }

    public static class CommonHeaderInterceptor implements RequestInterceptor {
        @Override
        public void apply(RequestTemplate template) {
            // 공통 헤더 추가
            template.header("Content-Type", "application/json");
            template.header("Accept", "application/json");
        }
    }
}