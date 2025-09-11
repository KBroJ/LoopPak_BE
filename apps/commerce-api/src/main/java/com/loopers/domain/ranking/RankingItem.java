package com.loopers.domain.ranking;

/**
 * 랭킹 아이템 Value Object (Domain Layer)
 *
 * 역할:
 * - Redis ZSET에서 조회한 개별 랭킹 데이터를 표현
 * - 불변 객체로 도메인 규칙 보장
 * - Infrastructure Layer에서 Redis 데이터 → Domain 객체 변환 시 사용
 *
 * 포함 정보:
 * - rank: 순위 (1위부터 시작)
 * - score: Redis ZSET 점수 (좋아요 가중치 누적)
 * - productId: 상품 식별자
 *
 */
public record RankingItem(
    int rank,
    double score,
    Long productId
) {

    /**
     * 정적 팩토리 메서드
     *
     * @param rank 순위 (1부터 시작)
     * @param score Redis ZSET 점수
     * @param productId 상품 ID
     * @return RankingItem 인스턴스
     */
    public static RankingItem of(int rank, double score, Long productId) {
        validateRank(rank);
        validateProductId(productId);

        return new RankingItem(rank, score, productId);
    }

    /**
     * 순위 유효성 검증
     *
     * @param rank 검증할 순위
     * @throws IllegalArgumentException 순위가 1보다 작으면 예외
     */
    private static void validateRank(int rank) {
        if (rank < 1) {
            throw new IllegalArgumentException("순위는 1보다 작을 수 없습니다: " + rank);
        }
    }

    /**
     * 상품 ID 유효성 검증
     *
     * @param productId 검증할 상품 ID
     * @throws IllegalArgumentException 상품 ID가 null이거나 0 이하이면 예외
     */
    private static void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("상품 ID는 양수여야 합니다: " + productId);
        }
    }

}
