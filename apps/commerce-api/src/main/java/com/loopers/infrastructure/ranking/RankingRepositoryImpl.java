package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MonthlyProductRanking;
import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingRepository;
import com.loopers.domain.ranking.WeeklyProductRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingRepositoryImpl implements RankingRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 관련 상수 (RankingEventHandler와 동일)
    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final String PRODUCT_MEMBER_PREFIX = "product:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WeeklyProductRankingJpaRepository weeklyProductRankingJpaRepository;
    private final MonthlyProductRankingJpaRepository monthlyProductRankingJpaRepository;

    /**
     * Top-N 랭킹 페이징 조회
     *
     * Redis 연산: ZREVRANGE WITH SCORES
     * - 높은 점수부터 내림차순 정렬
     * - 페이징: start = page * size, end = start + size - 1
     * - 점수와 함께 조회하여 RankingItem 생성
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param size 페이지 크기
     * @param page 페이지 번호 (0부터 시작)
     * @return 랭킹 아이템 목록 (순위, 점수, 상품ID 포함)
     */
    @Override
    public List<RankingItem> getTopRankings(String date, int size, int page) {
        try {
            String rankingKey = generateRankingKey(date);

            // 페이징 계산: start = page * size, end = start + size - 1
            long start = (long) page * size;
            long end = start + size - 1;

            log.debug("Redis ZREVRANGE 조회 - key: {}, start: {}, end: {}",
                    rankingKey, start, end);

            // ZREVRANGE WITH SCORES: 높은 점수부터 내림차순으로 조회
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            Set<ZSetOperations.TypedTuple<Object>> rankingsWithScores = zSetOps.reverseRangeWithScores(rankingKey, start, end);

            if (rankingsWithScores == null || rankingsWithScores.isEmpty()) {
                log.debug("랭킹 데이터 없음 - key: {}", rankingKey);
                return List.of();
            }

            // Redis 데이터를 RankingItem 도메인 객체로 변환
            List<RankingItem> rankingItems = new ArrayList<>();
            int rank = (int) start + 1; // 순위는 1부터 시작

            for (ZSetOperations.TypedTuple<Object> tuple : rankingsWithScores) {
                String member = (String) tuple.getValue();
                Double score = tuple.getScore();

                if (member != null && score != null) {
                    // "product:123" 형식에서 상품 ID 추출
                    Long productId = extractProductId(member);

                    if (productId != null) {
                        RankingItem rankingItem = RankingItem.of(rank, score, productId);
                        rankingItems.add(rankingItem);
                        rank++;
                    }
                }
            }

            log.debug("랭킹 조회 완료 - key: {}, 조회된 아이템 수: {}",
                    rankingKey, rankingItems.size());

            return rankingItems;

        } catch (Exception e) {
            log.error("랭킹 조회 실패 - date: {}, size: {}, page: {}, error: {}",
                    date, size, page, e.getMessage(), e);
            throw new RuntimeException("랭킹 조회 실패", e);
        }
    }

    /**
     * 전체 랭킹 상품 수 조회
     *
     * Redis 연산: ZCARD
     * - 집합에 포함된 멤버 수 조회
     * - 페이징 메타데이터 생성에 사용
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @return 랭킹에 포함된 총 상품 수
     */
    @Override
    public long getTotalRankingCount(String date) {
        try {
            String rankingKey = generateRankingKey(date);

            log.debug("Redis ZCARD 조회 - key: {}", rankingKey);

            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            Long count = zSetOps.zCard(rankingKey);

            long totalCount = count != null ? count : 0L;

            log.debug("전체 랭킹 수 조회 완료 - key: {}, count: {}",
                    rankingKey, totalCount);

            return totalCount;

        } catch (Exception e) {
            log.error("전체 랭킹 수 조회 실패 - date: {}, error: {}",
                    date, e.getMessage(), e);
            return 0L; // 에러 시 0 반환
        }
    }

    /**
     * 특정 상품의 현재 순위 조회
     *
     * Redis 연산: ZREVRANK
     * - 높은 점수부터의 순위 조회 (0부터 시작)
     * - 1을 더해서 사용자 친화적 순위 반환 (1부터 시작)
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param productId 상품 ID
     * @return 상품의 순위 (1부터 시작, 순위에 없으면 null)
     */
    @Override
    public Integer getProductRank(String date, Long productId) {
        try {
            String rankingKey = generateRankingKey(date);
            String member = PRODUCT_MEMBER_PREFIX + productId;

            log.debug("Redis ZREVRANK 조회 - key: {}, member: {}",
                    rankingKey, member);

            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            Long rank = zSetOps.reverseRank(rankingKey, member);

            if (rank == null) {
                log.debug("상품이 랭킹에 없음 - productId: {}", productId);
                return null; // 랭킹에 없으면 null
            }

            // Redis rank는 0부터 시작하므로 1을 더해 사용자 친화적으로 변환
            int userFriendlyRank = rank.intValue() + 1;

            log.debug("상품 순위 조회 완료 - productId: {}, rank: {}",
                    productId, userFriendlyRank);

            return userFriendlyRank;

        } catch (Exception e) {
            log.error("상품 순위 조회 실패 - date: {}, productId: {}, error: {}",
                    date, productId, e.getMessage(), e);
            return null; // 에러 시 null 반환
        }
    }

    /**
     * 특정 상품의 현재 점수 조회
     *
     * Redis 연산: ZSCORE
     * - 특정 멤버의 점수 조회
     * - 디버깅 및 모니터링 용도
     *
     * @param date 조회 날짜 (yyyyMMdd)
     * @param productId 상품 ID
     * @return 상품의 점수 (점수가 없으면 null)
     */
    @Override
    public Double getProductScore(String date, Long productId) {
        try {
            String rankingKey = generateRankingKey(date);
            String member = PRODUCT_MEMBER_PREFIX + productId;

            log.debug("Redis ZSCORE 조회 - key: {}, member: {}",
                    rankingKey, member);

            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            Double score = zSetOps.score(rankingKey, member);

            log.debug("상품 점수 조회 완료 - productId: {}, score: {}",
                    productId, score);

            return score; // null일 수 있음 (점수가 없는 경우)

        } catch (Exception e) {
            log.error("상품 점수 조회 실패 - date: {}, productId: {}, error: {}",
                    date, productId, e.getMessage(), e);
            return null; // 에러 시 null 반환
        }
    }

    /**
     * 날짜 기반 랭킹 키 생성
     *
     * @param date 날짜 (yyyyMMdd 형식)
     * @return Redis 키 (예: ranking:all:20250111)
     */
    private String generateRankingKey(String date) {
        return RANKING_KEY_PREFIX + date;
    }

    /**
     * Redis 멤버에서 상품 ID 추출
     *
     * @param member Redis 멤버 (예: "product:123")
     * @return 상품 ID (Long), 형식이 잘못되면 null
     */
    private Long extractProductId(String member) {
        try {
            if (member != null && member.startsWith(PRODUCT_MEMBER_PREFIX)) {
                String idStr = member.substring(PRODUCT_MEMBER_PREFIX.length());
                return Long.parseLong(idStr);
            }
            return null;
        } catch (NumberFormatException e) {
            log.warn("상품 ID 파싱 실패 - member: {}", member);
            return null;
        }
    }

    @Override
    public List<RankingItem> getWeeklyTopRankings(String yearWeek, int size, int page) {
        try {
            log.debug("주간 랭킹 조회 시작 - yearWeek: {}, size: {}, page: {}", yearWeek, size, page);

            // 1. JPA로 WeeklyProductRanking 페이징 조회
            Pageable pageable = PageRequest.of(page, size);
            Page<WeeklyProductRanking> weeklyRankings = weeklyProductRankingJpaRepository.findByYearWeekOrderByRankPositionAsc(yearWeek, pageable);

            // 2. Entity → Domain 객체 변환
            List<RankingItem> rankingItems = weeklyRankings.getContent().stream()
                    .map(entity -> RankingItem.of(
                            entity.getRankPosition(),                    // rank
                            entity.getRankingScore().doubleValue(),     // score
                            entity.getProductId()                       // productId
                    ))
                    .collect(Collectors.toList());

            log.debug("주간 랭킹 조회 완료 - yearWeek: {}, 조회된 아이템 수: {}", yearWeek, rankingItems.size());
            return rankingItems;

        } catch (Exception e) {
            log.error("주간 랭킹 조회 실패 - yearWeek: {}, size: {}, page: {}, error: {}",
                    yearWeek, size, page, e.getMessage(), e);
            throw new RuntimeException("주간 랭킹 조회 실패", e);
        }
    }

    @Override
    public long getTotalWeeklyRankingCount(String yearWeek) {
        try {
            log.debug("주간 랭킹 전체 수 조회 - yearWeek: {}", yearWeek);

            long count = weeklyProductRankingJpaRepository.countByYearWeek(yearWeek);

            log.debug("주간 랭킹 전체 수 조회 완료 - yearWeek: {}, count: {}", yearWeek, count);
            return count;

        } catch (Exception e) {
            log.error("주간 랭킹 전체 수 조회 실패 - yearWeek: {}, error: {}", yearWeek, e.getMessage(), e);
            return 0L; // 에러 시 0 반환
        }
    }

    @Override
    public List<RankingItem> getMonthlyTopRankings(String yearMonth, int size, int page) {
        try {
            log.debug("월간 랭킹 조회 시작 - yearMonth: {}, size: {}, page: {}", yearMonth, size, page);

            // 1. JPA로 MonthlyProductRanking 페이징 조회
            Pageable pageable = PageRequest.of(page, size);
            Page<MonthlyProductRanking> monthlyRankings =
                    monthlyProductRankingJpaRepository.findByYearMonthOrderByRankPositionAsc(yearMonth, pageable);

            // 2. Entity → Domain 객체 변환
            List<RankingItem> rankingItems = monthlyRankings.getContent().stream()
                    .map(entity -> RankingItem.of(
                            entity.getRankPosition(),                    // rank
                            entity.getRankingScore().doubleValue(),     // score
                            entity.getProductId()                       // productId
                    ))
                    .collect(Collectors.toList());

            log.debug("월간 랭킹 조회 완료 - yearMonth: {}, 조회된 아이템 수: {}", yearMonth, rankingItems.size());
            return rankingItems;

        } catch (Exception e) {
            log.error("월간 랭킹 조회 실패 - yearMonth: {}, size: {}, page: {}, error: {}",
                    yearMonth, size, page, e.getMessage(), e);
            throw new RuntimeException("월간 랭킹 조회 실패", e);
        }
    }

    @Override
    public long getTotalMonthlyRankingCount(String yearMonth) {
        try {
            log.debug("월간 랭킹 전체 수 조회 - yearMonth: {}", yearMonth);

            long count = monthlyProductRankingJpaRepository.countByYearMonth(yearMonth);

            log.debug("월간 랭킹 전체 수 조회 완료 - yearMonth: {}, count: {}", yearMonth, count);
            return count;

        } catch (Exception e) {
            log.error("월간 랭킹 전체 수 조회 실패 - yearMonth: {}, error: {}", yearMonth, e.getMessage(), e);
            return 0L; // 에러 시 0 반환
        }
    }

}
