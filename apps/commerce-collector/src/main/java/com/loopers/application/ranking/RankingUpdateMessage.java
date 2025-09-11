package com.loopers.application.ranking;

/**
 * 랭킹 업데이트를 위한 메시징 DTO
 * ProductMetrics 저장 후 Redis ZSET 적재를 위한 내부 통신용 메시지
 *
 * ex)
 * "상품123에 좋아요가 +1됐으니, Redis에서도 +0.2점 해줘!"
 * 라는 정보를 담는 택배상자
 */
public record RankingUpdateMessage(
        Long productId,                 // 어떤 상품인지 (예: 123L)
        RankingActionType actionType,   // 어떤 액션인지 (좋아요 추가, 좋아요 취소)
        String eventId                  // 중복 처리 방지용 고유번호
) {

    /**
     * 좋아요가 추가됐을 때 쓰는 메시지 생성기
     *
     * @param productId 상품 ID (예: 123L)
     * @param originalEventId 원래 이벤트의 ID (중복 방지용)
     * @return 완성된 메시지 객체
     */
    public static RankingUpdateMessage forLikeAdded(Long productId, String originalEventId) {
        return new RankingUpdateMessage(
                productId,
                RankingActionType.LIKE_ADDED,
                originalEventId + "_RANKING"
        );
    }

    /**
     * 좋아요가 취소됐을 때 쓰는 메시지 생성기
     *
     * @param productId 상품 ID
     * @param originalEventId 원래 이벤트의 ID
     * @return 완성된 메시지 객체
     */
    public static RankingUpdateMessage forLikeRemoved(Long productId, String originalEventId) {
        return new RankingUpdateMessage(
                productId,
                RankingActionType.LIKE_REMOVED,
                originalEventId + "_RANKING"
        );
    }

    /**
     * 점수 변화량 편의 메서드
     * 메시지에서 바로 점수를 가져올 수 있게!
     */
    public double getScoreChange() {
        return actionType.getScoreChange();
    }
}
