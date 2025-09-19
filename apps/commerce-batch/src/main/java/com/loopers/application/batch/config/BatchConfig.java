package com.loopers.application.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Spring Batch 기본 설정
 *
 * 역할:
 * - Spring Batch 기능 활성화
 * - JobRepository, JobLauncher, TransactionManager 등 자동 설정
 */
@Configuration
@EnableBatchProcessing  // Spring Batch 기능 활성화
public class BatchConfig {

    /**
     * 다른 앱들처럼 @Primary DataSource를 사용하도록 설정
     *
     * Spring Batch는 'dataSource' Bean 이름을 하드코딩으로 찾으므로
     * 기존 mySqlMainDataSource를 dataSource로 alias 생성
     */
    @Bean
    public DataSource dataSource(@Qualifier("mySqlMainDataSource") DataSource primaryDataSource) {
        return primaryDataSource;
    }

    /**
     * Spring Batch 메타데이터 테이블 자동 생성
     *
     * 필요한 이유:
     * - Spring Batch는 Job 실행 이력을 DB에 저장함 (BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION 등)
     * - application.yml의 initialize-schema: always가 제대로 작동하지 않는 경우 대안
     * - 개발 환경에서 테이블이 없을 때 자동으로 생성
     *
     * 생성되는 테이블들:
     * - BATCH_JOB_INSTANCE: Job 인스턴스 정보
     * - BATCH_JOB_EXECUTION: Job 실행 기록
     * - BATCH_JOB_EXECUTION_PARAMS: Job 실행 파라미터
     * - BATCH_STEP_EXECUTION: Step 실행 기록
     * - BATCH_JOB_EXECUTION_CONTEXT: Job 실행 컨텍스트
     * - BATCH_STEP_EXECUTION_CONTEXT: Step 실행 컨텍스트
     */
    @Bean
    public DataSourceInitializer dataSourceInitializer(@Qualifier("dataSource") DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        // Spring Batch에서 제공하는 MySQL용 스키마 파일 사용
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-mysql.sql"));
        populator.setContinueOnError(true); // 테이블이 이미 존재할 경우 에러 무시

        initializer.setDatabasePopulator(populator);
        return initializer;
    }

}