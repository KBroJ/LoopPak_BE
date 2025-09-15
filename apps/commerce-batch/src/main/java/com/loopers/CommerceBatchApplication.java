package com.loopers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

/**
 * Commerce Batch 애플리케이션
 *
 * 역할:
 * - Spring Batch Job 실행을 위한 메인 애플리케이션
 * - 주간/월간 랭킹 집계 배치 작업 수행
 * - 기존 commerce-api, commerce-collector와 동일한 DB/Redis 사용
 */
@RequiredArgsConstructor
@ConfigurationPropertiesScan
@EnableJpaRepositories(basePackages = "com.loopers.infrastructure")
@SpringBootApplication
public class CommerceBatchApplication{

    private final JobLauncher jobLauncher;  // 추가
    private final Job helloWorldJob;

    @PostConstruct
    public void started() {
        // 타임존 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        // Spring Boot 애플리케이션 실행
        // 배치 Job은 CommandLineRunner 또는 외부 스케줄러로 실행
        SpringApplication.run(CommerceBatchApplication.class, args);
    }

}