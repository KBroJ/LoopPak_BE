package com.loopers.application.batch.config;

import com.loopers.application.batch.processor.WeeklyRankingProcessor;
import com.loopers.application.batch.reader.ProductMetricsReader;
import com.loopers.application.batch.writer.WeeklyRankingWriter;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.ranking.WeeklyProductRanking;
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
 * 주간 상품 랭킹 배치 Job 설정
 *
 * 목적:
 * - ProductMetrics 테이블의 누적 데이터를 읽어서
 * - 주간 랭킹 점수를 계산하고
 * - TOP 100 랭킹을 mv_product_rank_weekly 테이블에 저장
 *
 * 실행 방법:
 * ./gradlew :apps:commerce-batch:bootRun --args="--job.name=weeklyRankingJob"
 *
 * 배치 흐름:
 * 1. ProductMetricsReader: product_metrics 테이블 전체 스캔
 * 2. WeeklyRankingProcessor: ProductMetrics → WeeklyProductRanking 변환
 * 3. WeeklyRankingWriter: 점수 정렬 → TOP 100 선별 → DB 저장
 *
 * 비즈니스 가치:
 * - 매주 정기적으로 실행하여 주간 인기 상품 랭킹 제공
 * - API에서 빠른 조회를 위한 Materialized View 생성
 * - Redis 일간 랭킹과 구분되는 장기 트렌드 분석 데이터
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklyRankingJobConfig {

    // Spring Batch 필수 의존성들
    private final JobRepository jobRepository;                      // Job 실행 이력 관리
    private final PlatformTransactionManager transactionManager;    // 트랜잭션 관리

    private final ProductMetricsReader productMetricsReader;
    private final WeeklyRankingProcessor weeklyRankingProcessor;
    private final WeeklyRankingWriter weeklyRankingWriter;

    /**
     * 주간 랭킹 Job 정의
     *
     * Job = 하나의 배치 업무 단위
     * - 이름: "weeklyRankingJob" (실행 시 --job.name으로 지정)
     * - RunIdIncrementer: 같은 파라미터로 여러 번 실행 가능하게 함
     * - 구성: weeklyRankingStep 하나로 구성
     *
     * @return 주간 랭킹 계산 Job
     */
    @Bean(name = "weeklyRankingJob")
    public Job weeklyRankingJob() {
        log.info("주간 랭킹 Job 설정 초기화");

        return new JobBuilder("weeklyRankingJob", jobRepository)
                // RunIdIncrementer: 매번 새로운 실행 ID 부여
                // → 같은 날에 여러 번 실행해도 중복 실행 오류 없음
                .incrementer(new RunIdIncrementer())

                // Step 추가: 실제 배치 작업 정의
                .start(weeklyRankingStep())

                // 향후 확장 가능: .next(anotherStep()) 으로 단계 추가 가능
                .build();
    }

    /**
     * 주간 랭킹 Step 정의
     *
     * Step = Job 내부의 실제 처리 단계
     * - Chunk-Oriented Processing: 청크 단위로 데이터 처리
     * - 청크 크기: 100개씩 (한 번에 100개 읽고 → 변환하고 → 저장)
     * - Reader → Processor → Writer 순서로 처리
     *
     * 왜 청크 처리인가?
     * - 메모리 효율: 전체 데이터를 한 번에 로드하지 않음
     * - 트랜잭션 관리: 청크 단위로 커밋/롤백
     * - 에러 복구: 실패한 청크만 재처리 가능
     *
     * @return 주간 랭킹 처리 Step
     */
    @Bean
    public Step weeklyRankingStep() {
        log.info("주간 랭킹 Step 설정 초기화");

        return new StepBuilder("weeklyRankingStep", jobRepository)
                // Chunk 설정: <INPUT_TYPE, OUTPUT_TYPE>chunk(size, transactionManager)
                .<ProductMetrics, WeeklyProductRanking>chunk(100, transactionManager)

                // Reader: 데이터를 어디서 읽을 것인가?
                .reader(createWeeklyRankingReader())

                // Processor: 읽은 데이터를 어떻게 변환할 것인가?
                .processor(weeklyRankingProcessor)

                // Writer: 변환된 데이터를 어디에 저장할 것인가?
                .writer(weeklyRankingWriter)

                // 추가 설정 가능:
                // .faultTolerant()           // 오류 허용 모드
                // .skipLimit(10)             // 최대 10개까지 스킵 허용
                // .skip(Exception.class)     // 특정 예외 시 스킵
                // .retryLimit(3)             // 재시도 횟수

                .build();
    }

    /**
     * Reader 생성 메서드
     *
     * 왜 별도 메서드로 분리했는가?
     * - ProductMetricsReader.createReader()는 ItemReader 인스턴스를 생성
     * - Bean으로 등록할 필요는 없고, Job 실행 시에만 생성하면 됨
     * - Step 설정에서 직접 호출하여 사용
     *
     * @return ProductMetrics를 읽는 ItemReader
     */
    private ItemReader<ProductMetrics> createWeeklyRankingReader() {
        log.debug("주간 랭킹 Reader 생성");

        // ProductMetricsReader에서 기본 Reader 생성
        // → product_metrics 테이블 전체를 productId 순으로 읽기
        return productMetricsReader.createReader();

        // 향후 확장 가능:
        // return productMetricsReader.createReaderWithFilter(minLikeCount);
        // return productMetricsReader.createTopNReader(1000);
    }

    /**
     * Job 실행 시 로그 출력용 정보
     *
     * 이 설정으로 생성되는 배치 플로우:
     *
     * 1. Job 시작: weeklyRankingJob
     * 2. Step 시작: weeklyRankingStep
     * 3. Reader: product_metrics 테이블에서 100개씩 읽기
     * 4. Processor: ProductMetrics → WeeklyProductRanking 변환 (100개)
     * 5. Writer: 100개를 정렬하여 상위 랭킹들을 DB에 저장
     * 6. 3-5 반복 (전체 데이터 처리 완료까지)
     * 7. Step 완료
     * 8. Job 완료
     *
     * 트랜잭션:
     * - 청크(100개) 단위로 트랜잭션 커밋
     * - 하나의 청크 처리 실패 시 해당 청크만 롤백
     * - 다른 청크는 영향 받지 않음
     */

}
