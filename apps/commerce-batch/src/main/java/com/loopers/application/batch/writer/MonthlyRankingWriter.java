package com.loopers.application.batch.writer;

import com.loopers.domain.ranking.MonthlyProductRanking;
import com.loopers.domain.ranking.MonthlyProductRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyRankingWriter implements ItemWriter<MonthlyProductRanking> {

    private final MonthlyProductRankingRepository monthlyRankingRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends MonthlyProductRanking> chunk) throws Exception {

        @SuppressWarnings("unchecked")
        List<MonthlyProductRanking> rankings = (List<MonthlyProductRanking>) chunk.getItems();

        // 1. 빈 청크 체크
        if (rankings.isEmpty()) {
            log.debug("빈 청크 수신 - 처리할 데이터 없음");
            return;
        }

        log.info("월간 랭킹 Writer 시작 - 처리할 아이템 수: {}", rankings.size());

        // 2. 랭킹 점수 기준으로 내림차순 정렬
        rankings.sort((ranking1, ranking2) ->
                ranking2.getRankingScore().compareTo(ranking1.getRankingScore()));

        log.debug("랭킹 점수 기준 정렬 완료");

        // 3. 순위 부여 및 TOP 100 선별
        List<MonthlyProductRanking> top100Rankings = IntStream.range(0, Math.min(100, rankings.size()))
                .mapToObj(index -> {
                    MonthlyProductRanking ranking = rankings.get(index);
                    int rankPosition = index + 1;

                    ranking.updateRankPosition(rankPosition);

                    log.debug("순위 부여 (월간): productId={}, rank={}, score={}", ranking.getProductId(), rankPosition, ranking.getRankingScore());

                    return ranking;
                })
                .collect(Collectors.toList());

        // 4. 해당 월 정보 추출
        String yearMonth = top100Rankings.get(0).getYearMonth();
        log.info("저장 대상 월: {}, TOP 100 랭킹 개수: {}", yearMonth, top100Rankings.size());

        // 5. 기존 데이터 삭제 (Replace 전략)
        int deletedCount = monthlyRankingRepository.deleteByYearMonth(yearMonth);
        log.info("기존 월간 랭킹 데이터 삭제 완료: {} 건", deletedCount);

        // 6. 새 TOP 100 랭킹 데이터 저장
        List<MonthlyProductRanking> savedRankings = monthlyRankingRepository.saveAll(top100Rankings);
        log.info("새 월간 랭킹 데이터 저장 완료: {} 건", savedRankings.size());

        // 7. 저장 결과 검증
        if (savedRankings.size() != top100Rankings.size()) {
            log.warn("저장 예상 개수와 실제 저장 개수 불일치 - 예상: {}, 실제: {}", top100Rankings.size(), savedRankings.size());
        }

        log.info("월간 랭킹 Writer 완료 - 월: {}, 저장된 랭킹: {}", yearMonth, savedRankings.size());
    }

}
