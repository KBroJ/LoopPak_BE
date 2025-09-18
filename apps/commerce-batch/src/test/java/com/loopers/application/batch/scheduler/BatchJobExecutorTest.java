package com.loopers.application.batch.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * BatchJobExecutor 단위 테스트
 *
 * 테스트 범위:
 * 1. Job 실행 성공/실패 시나리오
 * 2. 중복 실행 방지 검증
 * 3. 예외 처리 및 로깅 검증
 * 4. 동시성 상황에서의 안전성 검증
 *
 * 테스트 스타일: commerce-api와 동일
 * - @DisplayName으로 한글 설명
 * - AAA 패턴 (arrange, act, assert)
 * - @Nested로 기능별 그룹화
 * - Mockito + AssertJ 조합
 */
@ExtendWith(MockitoExtension.class)
class BatchJobExecutorTest {

    // 테스트 대상 (실제 인스턴스)
    private BatchJobExecutor batchJobExecutor;

    // Mock 의존성들
    @Mock private JobLauncher jobLauncher;
    @Mock private Job weeklyRankingJob;
    @Mock private Job monthlyRankingJob;

    @BeforeEach
    void setUp() {
        // BatchJobExecutor 인스턴스 생성
        // @RequiredArgsConstructor에 의해 생성된 생성자 사용
        batchJobExecutor = new BatchJobExecutor(jobLauncher, weeklyRankingJob, monthlyRankingJob);
    }

    @Nested
    @DisplayName("주간 랭킹 Job 실행")
    class ExecuteWeeklyRankingJob {

        @Test
        @DisplayName("정상적으로 주간 랭킹 Job을 실행하고 성공 결과를 반환한다")
        void returnsSuccessfulJobExecution_whenExecutingWeeklyRankingJob() throws Exception {
            // arrange
            JobExecution mockJobExecution = createSuccessfulJobExecution();
            given(jobLauncher.run(eq(weeklyRankingJob), any(JobParameters.class)))
                    .willReturn(mockJobExecution);

            // act
            JobExecution result = batchJobExecutor.executeWeeklyRankingJob();

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            verify(jobLauncher, times(1)).run(eq(weeklyRankingJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("JobLauncher에서 예외 발생 시 RuntimeException으로 래핑하여 던진다")
        void throwsRuntimeException_whenJobLauncherThrowsException() throws Exception {
            // arrange - JobLauncher.run()은 RuntimeException을 던질 수 있음
            RuntimeException jobException = new RuntimeException("Job 실행 실패");
            given(jobLauncher.run(eq(weeklyRankingJob), any(JobParameters.class)))
                    .willThrow(jobException);

            // act & assert
            assertThatThrownBy(() -> batchJobExecutor.executeWeeklyRankingJob())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("WeeklyRanking Job 실행 실패")
                    .hasCause(jobException);
        }
    }

    @Nested
    @DisplayName("월간 랭킹 Job 실행")
    class ExecuteMonthlyRankingJob {

        @Test
        @DisplayName("정상적으로 월간 랭킹 Job을 실행하고 성공 결과를 반환한다")
        void returnsSuccessfulJobExecution_whenExecutingMonthlyRankingJob() throws Exception {
            // arrange
            JobExecution mockJobExecution = createSuccessfulJobExecution();
            given(jobLauncher.run(eq(monthlyRankingJob), any(JobParameters.class)))
                    .willReturn(mockJobExecution);

            // act
            JobExecution result = batchJobExecutor.executeMonthlyRankingJob();

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            verify(jobLauncher, times(1)).run(eq(monthlyRankingJob), any(JobParameters.class));
        }
    }

    @Nested
    @DisplayName("중복 실행 방지")
    class DuplicateExecutionPrevention {

        @Test
        @DisplayName("동일한 Job 타입의 중복 실행은 방지된다")
        void preventsDuplicateExecution_ofSameJobType() throws Exception {
            // arrange
            int threadCount = 3;
            CountDownLatch startLatch = new CountDownLatch(1);  // 모든 스레드 동시 시작
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // JobLauncher가 시간이 걸리도록 설정
            given(jobLauncher.run(eq(weeklyRankingJob), any(JobParameters.class)))
                    .willAnswer(invocation -> {
                        Thread.sleep(100); // 100ms 소요
                        return createSuccessfulJobExecution();
                    });

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            // act - 동시에 같은 Job 실행 시도
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await(); // 모든 스레드가 동시에 시작
                        batchJobExecutor.executeWeeklyRankingJob();
                        successCount.incrementAndGet();
                    } catch (IllegalStateException e) {
                        if (e.getMessage().contains("이미 실행 중입니다")) {
                            failureCount.incrementAndGet(); // 중복 실행 방지 (예상된 동작)
                        }
                    } catch (Exception e) {
                        // 다른 예외는 발생하면 안됨
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 모든 스레드 시작 신호
            endLatch.await(); // 모든 스레드 완료 대기

            // assert
            assertThat(successCount.get()).isEqualTo(1); // 하나만 성공
            assertThat(failureCount.get()).isEqualTo(threadCount - 1); // 나머지는 중복 실행 방지

            // JobLauncher는 성공한 1번만 호출되어야 함
            verify(jobLauncher, times(1)).run(eq(weeklyRankingJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("주간 Job과 월간 Job은 독립적으로 동시 실행 가능하다")
        void allowsConcurrentExecution_ofDifferentJobTypes() throws Exception {
            // arrange
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);

            given(jobLauncher.run(eq(weeklyRankingJob), any(JobParameters.class)))
                    .willAnswer(invocation -> {
                        Thread.sleep(50);
                        return createSuccessfulJobExecution();
                    });

            given(jobLauncher.run(eq(monthlyRankingJob), any(JobParameters.class)))
                    .willAnswer(invocation -> {
                        Thread.sleep(50);
                        return createSuccessfulJobExecution();
                    });

            ExecutorService executorService = Executors.newFixedThreadPool(2);

            // act - 서로 다른 Job 동시 실행
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    JobExecution result = batchJobExecutor.executeWeeklyRankingJob();
                    if (result.getStatus() == BatchStatus.COMPLETED) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    startLatch.await();
                    JobExecution result = batchJobExecutor.executeMonthlyRankingJob();
                    if (result.getStatus() == BatchStatus.COMPLETED) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });

            startLatch.countDown();
            endLatch.await();

            // assert
            assertThat(successCount.get()).isEqualTo(2); // 둘 다 성공해야 함
            verify(jobLauncher, times(1)).run(eq(weeklyRankingJob), any(JobParameters.class));
            verify(jobLauncher, times(1)).run(eq(monthlyRankingJob), any(JobParameters.class));
        }
    }

    /**
     * Test Helper 메서드들
     */
    private JobExecution createSuccessfulJobExecution() {
        JobExecution jobExecution = mock(JobExecution.class);
        given(jobExecution.getStatus()).willReturn(BatchStatus.COMPLETED);
        given(jobExecution.getStartTime()).willReturn(LocalDateTime.now().minusMinutes(1));
        given(jobExecution.getEndTime()).willReturn(LocalDateTime.now());

        // StepExecution Mock (로깅에서 사용되는 부분만)
        StepExecution stepExecution = mock(StepExecution.class);
        given(stepExecution.getStepName()).willReturn("testStep");
        given(stepExecution.getReadCount()).willReturn(100L);
        given(stepExecution.getWriteCount()).willReturn(100L);
        given(stepExecution.getCommitCount()).willReturn(1L);
        given(stepExecution.getSkipCount()).willReturn(0L);

        given(jobExecution.getStepExecutions()).willReturn(List.of(stepExecution));
        return jobExecution;
    }
}