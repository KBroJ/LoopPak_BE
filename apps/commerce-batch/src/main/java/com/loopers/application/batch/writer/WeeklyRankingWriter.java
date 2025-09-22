package com.loopers.application.batch.writer;

import com.loopers.domain.ranking.WeeklyProductRanking;
import com.loopers.domain.ranking.WeeklyProductRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 주간 상품 랭킹을 Materialized View 테이블에 저장하는 Writer
 *
 * 역할:
 * - Processor에서 변환된 WeeklyProductRanking 청크 단위 처리
 * - 랭킹 점수 기준으로 정렬 (높은 점수 우선)
 * - 순위 부여 (1, 2, 3, ... 순서)
 * - TOP 100 랭킹만 선별하여 저장
 * - 기존 주차 데이터 삭제 후 새 데이터 저장 (Replace 전략)
 *
 * Spring Batch 흐름:
 * Reader → Processor → [이 Writer] → Database
 *
 * 트랜잭션:
 * - 청크 단위로 트랜잭션 처리
 * - 실패 시 해당 청크 전체 롤백
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyRankingWriter implements ItemWriter<WeeklyProductRanking> {

    private final WeeklyProductRankingRepository weeklyRankingRepository;

    /**
     * 주간 랭킹 데이터를 정렬하여 TOP 100을 Materialized View에 저장
     *
     * @param chunk Processor에서 변환된 WeeklyProductRanking 목록 (청크 단위)
     * @throws Exception 저장 중 예외 발생 시
     */
    @Override
    @Transactional
    public void write(Chunk<? extends WeeklyProductRanking> chunk) throws Exception {

        @SuppressWarnings("unchecked")
        List<WeeklyProductRanking> rankings = (List<WeeklyProductRanking>) chunk.getItems();

        // 1. 빈 청크 체크
        if (rankings.isEmpty()) {
            log.debug("빈 청크 수신 - 처리할 데이터 없음");
            return;
        }

        log.info("주간 랭킹 Writer 시작 - 처리할 아이템 수: {}", rankings.size());

        // 2. 랭킹 점수 기준으로 내림차순 정렬 (높은 점수가 1등)
        rankings.sort((ranking1, ranking2) ->
                ranking2.getRankingScore().compareTo(ranking1.getRankingScore()));

        log.debug("랭킹 점수 기준 정렬 완료");

        // 3. 순위 부여 및 TOP 100 선별
        List<WeeklyProductRanking> top100Rankings = IntStream.range(0, Math.min(100, rankings.size()))
                .mapToObj(index -> {
                    WeeklyProductRanking ranking = rankings.get(index);
                    int rankPosition = index + 1; // 1부터 시작하는 순위

                    // 순위 업데이트
                    ranking.updateRankPosition(rankPosition);

                    log.debug("순위 부여: productId={}, rank={}, score={}",
                            ranking.getProductId(), rankPosition, ranking.getRankingScore());

                    return ranking;
                })
                .collect(Collectors.toList());

        // 4. 해당 주차 정보 추출 (모든 아이템이 같은 주차여야 함)
        String yearWeek = top100Rankings.get(0).getYearWeek();
        log.info("저장 대상 주차: {}, TOP 100 랭킹 개수: {}", yearWeek, top100Rankings.size());

        // 5. 기존 데이터 삭제 (Replace 전략)
        // 동일 주차의 기존 랭킹 데이터를 모두 삭제하여 중복 방지
        int deletedCount = weeklyRankingRepository.deleteByYearWeek(yearWeek);
        log.info("기존 주간 랭킹 데이터 삭제 완료: {} 건", deletedCount);

        // 6. 새 TOP 100 랭킹 데이터 저장
        List<WeeklyProductRanking> savedRankings = weeklyRankingRepository.saveAll(top100Rankings);
        log.info("새 주간 랭킹 데이터 저장 완료: {} 건", savedRankings.size());

        // 7. 저장 결과 검증
        if (savedRankings.size() != top100Rankings.size()) {
            log.warn("저장 예상 개수와 실제 저장 개수 불일치 - 예상: {}, 실제: {}",
                    top100Rankings.size(), savedRankings.size());
        }

        log.info("주간 랭킹 Writer 완료 - 주차: {}, 저장된 랭킹: {}", yearWeek, savedRankings.size());
    }

}
