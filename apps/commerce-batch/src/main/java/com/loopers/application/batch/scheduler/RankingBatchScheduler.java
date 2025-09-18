package com.loopers.application.batch.scheduler;

/**
 * 랭킹 배치 스케줄러
 *
 * 역할:
 * 1. application.yml의 Cron 설정에 따라 주간/월간 랭킹 배치 자동 실행
 * 2. 환경별 스케줄러 활성화/비활성화 제어
 * 3. BatchJobExecutor에 실제 Job 실행 위임
 * 4. 스케줄링 관련 로깅 및 모니터링
 *
 * 조건부 활성화:
 * - batch.scheduler.enabled=true 일 때만 Bean 생성
 * - false이거나 설정이 없으면 스케줄러 비활성화
 *
 * 환경별 동작:
 * - local/dev: enabled=false → 스케줄러 Bean 생성 안됨 → 수동 실행만
 * - qa: enabled=true → 6시간/12시간마다 실행 → 스케줄링 테스트
 * - prd: enabled=true → 월요일 3시/1일 4시 실행 → 실제 운영
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "batch.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = false  // 설정이 없으면 비활성화 (안전)
)
public class RankingBatchScheduler {

    // BatchJobExecutor 의존성 주입
    // 실제 Job 실행 로직은 BatchJobExecutor에서 담당
    private final BatchJobExecutor batchJobExecutor;

    /**
     * 주간 랭킹 배치 스케줄링
     *
     * 실행 조건:
     * - batch.scheduler.enabled=true
     * - Cron 표현식: ${batch.scheduler.weekly.cron}
     *
     */
    @Scheduled(cron = "${batch.scheduler.weekly.cron}")
    public void executeWeeklyRankingBatch() {
        log.info("스케줄된 주간 랭킹 배치 실행 시작");
        log.info("실행 시점: 매주 설정된 시간 (현재 Cron: ${batch.scheduler.weekly.cron})");

        try {
            // 실행 전 환경 정보 로깅
            logScheduledExecutionInfo("주간 랭킹");

            // BatchJobExecutor에 실제 Job 실행 위임
            // 중복 실행 방지, 예외 처리, 상세 로깅은 BatchJobExecutor에서 처리
            JobExecution jobExecution = batchJobExecutor.executeWeeklyRankingJob();

            // 스케줄된 실행 결과 로깅
            logScheduledExecutionResult("주간 랭킹", jobExecution, true);

        } catch (Exception e) {
            // 스케줄된 실행 실패 로깅
            log.error("스케줄된 주간 랭킹 배치 실행 실패");
            log.error("실패 원인: {}", e.getMessage(), e);

            // 스케줄러 레벨에서의 실패 처리
            handleScheduledExecutionFailure("주간 랭킹", e);
        }

        log.info("🏁 스케줄된 주간 랭킹 배치 실행 완료");
    }

    /**
     * 월간 랭킹 배치 스케줄링
     *
     * 실행 조건:
     * - batch.scheduler.enabled=true
     * - Cron 표현식: ${batch.scheduler.monthly.cron}
     *
     */
    @Scheduled(cron = "${batch.scheduler.monthly.cron}")
    public void executeMonthlyRankingBatch() {
        log.info("스케줄된 월간 랭킹 배치 실행 시작");
        log.info("실행 시점: 매월 설정된 시간 (현재 Cron: ${batch.scheduler.monthly.cron})");

        try {
            // 실행 전 환경 정보 로깅
            logScheduledExecutionInfo("월간 랭킹");

            // BatchJobExecutor에 실제 Job 실행 위임
            JobExecution jobExecution = batchJobExecutor.executeMonthlyRankingJob();

            // 스케줄된 실행 결과 로깅
            logScheduledExecutionResult("월간 랭킹", jobExecution, true);

        } catch (Exception e) {
            // 스케줄된 실행 실패 로깅
            log.error("스케줄된 월간 랭킹 배치 실행 실패");
            log.error("실패 원인: {}", e.getMessage(), e);

            // 스케줄러 레벨에서의 실패 처리
            handleScheduledExecutionFailure("월간 랭킹", e);
        }

        log.info("🏁 스케줄된 월간 랭킹 배치 실행 완료");
    }

    /**
     * 스케줄된 실행 정보 로깅
     *
     * 스케줄러에서 실행되는 배치의 환경 정보를 로깅하여
     * 운영 시 실행 상황을 모니터링할 수 있도록 함
     *
     * @param batchType 배치 타입 (주간/월간)
     */
    private void logScheduledExecutionInfo(String batchType) {
        log.info("  {} 배치 스케줄 실행 정보:", batchType);
        log.info("   └ 실행 방식: 자동 스케줄링 (Spring @Scheduled)");
        log.info("   └ 실행 환경: {} Profile", System.getProperty("spring.profiles.active", "default"));
        log.info("   └ 스케줄러 상태: batch.scheduler.enabled=true");
        log.info("   └ JVM 메모리: {}MB / {}MB",
                Runtime.getRuntime().totalMemory() / 1024 / 1024,
                Runtime.getRuntime().maxMemory() / 1024 / 1024);
    }

    /**
     * 스케줄된 실행 결과 로깅
     *
     * BatchJobExecutor에서 반환된 JobExecution 결과를
     * 스케줄러 관점에서 추가 로깅
     *
     * @param batchType 배치 타입
     * @param jobExecution Job 실행 결과
     * @param isScheduled 스케줄된 실행 여부 (true: 스케줄, false: 수동)
     */
    private void logScheduledExecutionResult(String batchType, JobExecution jobExecution, boolean isScheduled) {
        String executionType = isScheduled ? "스케줄된" : "수동";

        log.info("  {} {} 배치 실행 결과:", executionType, batchType);
        log.info("   └ Job 상태: {}", jobExecution.getStatus());
        log.info("   └ Job ID: {}", jobExecution.getId());
        log.info("   └ 다음 스케줄: 설정된 Cron 표현식에 따라 자동 실행");

        // 성공 시 추가 정보
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.warn("{} 배치 실행이 완전히 성공하지 못했습니다.", batchType);
            log.warn("   상세 내용은 BatchJobExecutor 로그를 확인하세요.");
        }
    }

    /**
     * 스케줄된 실행 실패 처리
     *
     * 스케줄러 레벨에서 발생한 실패에 대한 추가 처리
     * (알림, 모니터링, 재시도 등 향후 확장 가능)
     *
     * @param batchType 배치 타입
     * @param exception 발생한 예외
     */
    private void handleScheduledExecutionFailure(String batchType, Exception exception) {
        log.error(" 스케줄러 레벨 실패 처리: {}", batchType);
        log.error("   └ 예외 타입: {}", exception.getClass().getSimpleName());
        log.error("   └ 다음 스케줄: 설정된 시간에 자동 재시도");

        // 향후 확장 가능한 실패 처리 로직:
        // - 슬랙/이메일 알림 발송
        // - 모니터링 시스템에 메트릭 전송
        // - 실패 카운터 증가
        // - 관리자 대시보드에 알림 표시

        // 현재는 로깅만 수행, 스케줄러는 계속 동작
        log.info(" 스케줄러는 계속 동작하며 다음 설정 시간에 재시도합니다.");
    }

}
