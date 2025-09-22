package com.loopers.application.batch.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Hello World를 출력하는 간단한 Tasklet
 *
 * Tasklet이란?
 * - Step 내에서 실행되는 단일 작업 단위
 * - Reader/Processor/Writer 없이 단순한 작업 수행
 * - execute() 메서드에서 비즈니스 로직 구현
 *
 * 사용 사례:
 * - 파일 정리, 데이터 삭제 등 간단한 작업
 * - 복잡한 SQL 쿼리 한 번 실행
 * - 외부 API 호출 등
 */
@Slf4j
@Component
public class HelloWorldTasklet implements Tasklet {

    /**
     * Tasklet의 핵심 메서드
     *
     * @param contribution Step 실행 정보 (실행 횟수, 상태 등)
     * @param chunkContext Chunk 실행 컨텍스트 (Job Parameters 접근 등)
     * @return RepeatStatus.FINISHED - 작업 완료
     *         RepeatStatus.CONTINUABLE - 작업 계속 (반복 실행)
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 1. Job Parameters 접근해보기
        String message = chunkContext.getStepContext()
                .getJobParameters()
                .getOrDefault("message", "Hello World from Spring Batch!").toString();

        // 2. 현재 Step 정보 로그
        String jobName = chunkContext.getStepContext().getJobName();
        String stepName = chunkContext.getStepContext().getStepName();

        log.info("=== Batch Job 실행 중 ===");
        log.info("Job Name: {}", jobName);
        log.info("Step Name: {}", stepName);
        log.info("Message: {}", message);
        log.info("실행 시간: {}", java.time.LocalDateTime.now());

        // 3. 간단한 비즈니스 로직 시뮬레이션
        Thread.sleep(1000); // 1초 대기 (실제 작업 시뮬레이션)

        log.info("=== Hello World Batch 작업 완료! ===");

        // 4. 작업 완료 신호 반환
        return RepeatStatus.FINISHED;
    }

}
