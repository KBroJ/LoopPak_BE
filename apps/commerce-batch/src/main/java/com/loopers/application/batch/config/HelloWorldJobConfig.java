package com.loopers.application.batch.config;

import com.loopers.application.batch.tasklet.HelloWorldTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Hello World 배치 Job 설정
 *
 * Spring Batch Job 구조:
 * Job (전체 작업)
 *  └── Step (작업 단계)
 *       └── Tasklet (실제 작업 로직)
 */
@Slf4j
//@Configuration
@RequiredArgsConstructor
public class HelloWorldJobConfig {

    // Spring Boot가 자동으로 주입해주는 Bean들
    private final JobRepository jobRepository;              // Job 실행 이력 관리
    private final PlatformTransactionManager txManager;     // 트랜잭션 관리
    private final HelloWorldTasklet helloWorldTasklet;

    /**
     * Hello World Job 정의
     *
     * Job이란?
     * - 배치 작업의 전체 단위
     * - 여러 개의 Step으로 구성
     * - 한 번 실행되면 JobInstance 생성
     * - 같은 JobParameter로 재실행 불가 (기본값)
     */
    @Bean
    public Job helloWorldJob() {
        return new JobBuilder("helloWorldJob", jobRepository)  // Job 이름 설정
                .start(helloWorldStep())        // 첫 번째 Step 설정
                // .next(anotherStep())         // 다음 Step (필요시)
                .build();
    }

    /**
     * Hello World Step 정의
     *
     * Step이란?
     * - Job을 구성하는 작업 단위
     * - Tasklet 또는 Chunk 기반으로 구현
     * - 각각 독립적인 트랜잭션으로 실행
     */
    @Bean
    public Step helloWorldStep() {
        return new StepBuilder("helloWorldStep", jobRepository)  // Step 이름 설정
                .tasklet(helloWorldTasklet, txManager)
                .build();
    }

}
