package com.loopers.application.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 배치 Job 실행을 담당하는 핵심 컴포넌트
 *
 * 역할:
 * 1. Job 안전 실행: 중복 실행 방지, 예외 처리, 상세 로깅
 * 2. 공통 로직 제공: Job Parameters 생성, 실행 결과 추적
 * 3. 운영 지원: 모니터링 가능한 로그, 실행 상태 관리
 *
 * 설계 원칙:
 * - Template Method: executeJobSafely()로 공통 실행 패턴 제공
 * - Thread Safety: AtomicBoolean로 동시 실행 제어
 * - Fail Fast: 문제 발생 시 빠른 실패 및 명확한 에러 메시지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobExecutor {

    // Spring Batch 핵심 컴포넌트
    private final JobLauncher jobLauncher;

    // Job 의존성 주입 (Bean 이름으로 구분)
    // @Qualifier를 사용하는 이유: 같은 타입(Job)의 여러 Bean이 존재할 때 명시적 지정
    @Qualifier("weeklyRankingJob")
    private final Job weeklyRankingJob;

    @Qualifier("monthlyRankingJob")
    private final Job monthlyRankingJob;

    // 중복 실행 방지를 위한 플래그
    // AtomicBoolean을 사용하는 이유: 멀티스레드 환경에서 안전한 상태 변경 보장
    private final AtomicBoolean weeklyJobRunning = new AtomicBoolean(false);
    private final AtomicBoolean monthlyJobRunning = new AtomicBoolean(false);

    /**
     * 주간 랭킹 배치 Job 실행
     *
     * 실행 시점: 매주 월요일 새벽 3시 (스케줄러에서 호출)
     * 처리 내용: product_metrics → mv_product_rank_weekly (TOP 100)
     *
     * @return JobExecution 실행 결과 (성공/실패 상태, 처리 건수 등)
     * @throws IllegalStateException 이미 실행 중인 경우
     * @throws RuntimeException Job 실행 실패 시
     */
    public JobExecution executeWeeklyRankingJob() {
        return executeJobSafely(weeklyRankingJob, weeklyJobRunning, "WeeklyRanking");
    }

    /**
     * 월간 랭킹 배치 Job 실행
     *
     * 실행 시점: 매월 1일 새벽 4시 (스케줄러에서 호출)
     * 처리 내용: product_metrics → mv_product_rank_monthly (TOP 100)
     *
     * @return JobExecution 실행 결과
     * @throws IllegalStateException 이미 실행 중인 경우
     * @throws RuntimeException Job 실행 실패 시
     */
    public JobExecution executeMonthlyRankingJob() {
        return executeJobSafely(monthlyRankingJob, monthlyJobRunning, "MonthlyRanking");
    }

    /**
     * Job 안전 실행을 위한 Template Method
     *
     * 실행 흐름:
     * 1. 중복 실행 체크 (compareAndSet 원자적 연산)
     * 2. Job Parameters 생성 (실행마다 고유값)
     * 3. Job 실행 (JobLauncher.run())
     * 4. 실행 결과 로깅 (성공/실패, 처리 건수, 소요 시간)
     * 5. 예외 처리 및 플래그 해제 (finally 블록)
     *
     * 왜 Template Method인가?
     * - 모든 Job 실행의 공통 패턴을 한 곳에서 관리
     * - 중복 코드 제거 및 일관된 실행 방식 보장
     * - 운영 관점에서 예측 가능한 로그 형식
     *
     * @param job 실행할 Spring Batch Job
     * @param runningFlag 실행 상태를 추적하는 AtomicBoolean
     * @param jobName 로깅용 Job 이름
     * @return JobExecution 실행 결과
     */
    private JobExecution executeJobSafely(Job job, AtomicBoolean runningFlag, String jobName) {

        // 1단계: 중복 실행 방지
        // compareAndSet(false, true): 현재 값이 false면 true로 변경하고 true 반환
        // 이미 true라면 변경하지 않고 false 반환 → 중복 실행 감지
        if (!runningFlag.compareAndSet(false, true)) {
            String errorMessage = jobName + " Job이 이미 실행 중입니다. 중복 실행을 건너뜁니다.";
            log.warn(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        try {
            log.info("=== {} Job 실행 시작 ===", jobName);

            // 2단계: Job Parameters 생성
            // Spring Batch는 같은 JobParameters로 재실행을 방지함
            // 따라서 매번 고유한 파라미터를 생성해야 함
            JobParameters jobParameters = createJobParameters();
            log.debug("Job Parameters 생성 완료: {}", jobParameters.getParameters());

            // 3단계: Job 실행
            // JobLauncher.run()은 동기적으로 실행됨 (완료까지 대기)
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            // 4단계: 실행 결과 로깅
            logJobResult(jobExecution, jobName);

            return jobExecution;

        } catch (Exception e) {
            // Job 실행 중 발생한 모든 예외를 잡아서 명확한 에러 메시지와 함께 재발생
            log.error("=== {} Job 실행 실패 ===", jobName);
            log.error("실패 원인: {}", e.getMessage(), e);
            throw new RuntimeException(jobName + " Job 실행 실패", e);

        } finally {
            // 5단계: 실행 플래그 해제 (성공/실패 관계없이 반드시 실행)
            // finally 블록을 사용하는 이유: 예외 발생 시에도 플래그 해제 보장
            runningFlag.set(false);
            log.info("=== {} Job 실행 완료 - 실행 플래그 해제 ===", jobName);
        }
    }

    /**
     * Job Parameters 생성
     *
     * Spring Batch Job Parameters의 역할:
     * 1. Job Instance 구분: 같은 Job이라도 다른 파라미터면 별개 실행으로 인식
     * 2. 재실행 방지: 동일한 파라미터로는 재실행 불가 (JobInstanceAlreadyCompleteException)
     * 3. 파라미터 전달: Job 내부에서 사용할 값들 전달
     *
     * 우리가 사용하는 파라미터:
     * - timestamp: 실행 시점의 밀리초 (고유성 보장)
     * - runId: UUID (추가적인 고유성 보장)
     *
     * @return JobParameters 실행마다 고유한 파라미터
     */
    private JobParameters createJobParameters() {
        return new JobParametersBuilder()
                // 실행 시점의 timestamp로 고유성 보장
                .addLong("timestamp", System.currentTimeMillis())
                // 추가적인 고유성을 위한 UUID
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters();
    }

    /**
     * Job 실행 결과 상세 로깅
     *
     * 로깅하는 정보:
     * 1. 실행 상태 (COMPLETED, FAILED, STOPPED 등)
     * 2. 실행 시간 (시작, 종료, 소요 시간)
     * 3. Step별 처리 통계 (읽기, 처리, 쓰기 건수)
     * 4. 실패 시 원인 (Exception 상세 정보)
     *
     * 왜 이렇게 상세히 로깅하는가?
     * - 운영 모니터링: 배치 실행 상태를 실시간으로 파악
     * - 성능 분석: 소요 시간, 처리 건수로 성능 추이 분석
     * - 문제 해결: 실패 시 빠른 원인 파악 및 대응
     *
     * @param jobExecution Spring Batch Job 실행 결과
     * @param jobName 로깅용 Job 이름
     */
    private void logJobResult(JobExecution jobExecution, String jobName) {
        BatchStatus status = jobExecution.getStatus();
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();

        if (status == BatchStatus.COMPLETED) {
            // 성공 시 로깅
            long duration = Duration.between(startTime, endTime).toMillis();
            log.info("{} Job 실행 성공", jobName);
            log.info("   소요시간: {}ms", duration);
            log.info("   시작: {}", startTime);
            log.info("   종료: {}", endTime);

            // Step별 처리 통계 로깅
            // getStepExecutions(): Job 내의 모든 Step 실행 결과
            jobExecution.getStepExecutions().forEach(stepExecution -> {
                log.info("Step: {} | 읽기: {}건 | 쓰기: {}건 | 커밋: {}회 | 스킵: {}건",
                        stepExecution.getStepName(),        // Step 이름
                        stepExecution.getReadCount(),       // 읽은 아이템 수
                        stepExecution.getWriteCount(),      // 쓴 아이템 수
                        stepExecution.getCommitCount(),     // 커밋 횟수
                        stepExecution.getSkipCount());      // 스킵된 아이템 수
            });

        } else {
            // 실패 시 로깅
            log.error("{} Job 실행 실패", jobName);
            log.error("   상태: {}", status);
            log.error("   시작: {}", startTime);
            log.error("   종료: {}", endTime);

            // 실패 원인 상세 로깅
            // getAllFailureExceptions(): Job 실행 중 발생한 모든 예외
            jobExecution.getAllFailureExceptions().forEach(exception -> {
                log.error("실패 원인: {}", exception.getMessage(), exception);
            });
        }
    }

}
