package com.loopers.application.batch.config;

import com.loopers.application.batch.processor.MonthlyRankingProcessor;
import com.loopers.application.batch.reader.ProductMetricsReader;
import com.loopers.application.batch.writer.MonthlyRankingWriter;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.ranking.MonthlyProductRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 월간 상품 랭킹 배치 Job 설정
 *
 * 목적:
 * - ProductMetrics 테이블의 누적 데이터를 읽어서
 * - 월간 랭킹 점수를 계산하고
 * - TOP 100 랭킹을 mv_product_rank_monthly 테이블에 저장
 *
 * 실행 방법:
 * ./gradlew :apps:commerce-batch:bootRun --args="--job.name=monthlyRankingJob"
 *
 * 배치 흐름:
 * 1. ProductMetricsReader: product_metrics 테이블 전체 스캔
 * 2. MonthlyRankingProcessor: ProductMetrics → MonthlyProductRanking 변환
 * 3. MonthlyRankingWriter: 점수 정렬 → TOP 100 선별 → DB 저장
 *
 * 비즈니스 가치:
 * - 매월 정기적으로 실행하여 월간 인기 상품 랭킹 제공
 * - API에서 빠른 조회를 위한 Materialized View 생성
 * - 장기 트렌드 분석을 위한 월단위 집계 데이터
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlyRankingJobConfig {

    // Spring Batch 필수 의존성들
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 월간 랭킹 전용 컴포넌트들
    private final ProductMetricsReader productMetricsReader;
    private final MonthlyRankingProcessor monthlyRankingProcessor;
    private final MonthlyRankingWriter monthlyRankingWriter;

    /**
     * 월간 랭킹 Job 정의
     *
     * @return 월간 랭킹 계산 Job
     */
    @Bean(name = "monthlyRankingJob")
    public Job monthlyRankingJob() {
        log.info("월간 랭킹 Job 설정 초기화");

        return new JobBuilder("monthlyRankingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(monthlyRankingStep())
                .build();
    }

    /**
     * 월간 랭킹 Step 정의
     *
     * @return 월간 랭킹 처리 Step
     */
    @Bean
    public Step monthlyRankingStep() {
        log.info("월간 랭킹 Step 설정 초기화");

        return new StepBuilder("monthlyRankingStep", jobRepository)
                .<ProductMetrics, MonthlyProductRanking>chunk(100, transactionManager)
                .reader(createMonthlyRankingReader())
                .processor(monthlyRankingProcessor)
                .writer(monthlyRankingWriter)
                .build();
    }

    /**
     * 월간 랭킹용 Reader 생성
     *
     * @return ProductMetrics를 읽는 ItemReader
     */
    private ItemReader<ProductMetrics> createMonthlyRankingReader() {
        log.debug("월간 랭킹 Reader 생성");
        return productMetricsReader.createReader();
    }

}
