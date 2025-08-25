package com.loopers.config.feign;

import feign.Request;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTimeoutConfig {

    @Bean
    public Request.Options feignRequestOptions(FeignProperties feignProperties) {
        return new Request.Options(
                feignProperties.getConnectTimeout(),
                feignProperties.getReadTimeout()
        );
    }

    @ConfigurationProperties(prefix = "feign")
    public static class FeignProperties {
        private int connectTimeout = 3000;  // 3초
        private int readTimeout = 10000;    // 10초

        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
        public int getReadTimeout() { return readTimeout; }
        public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    }
}