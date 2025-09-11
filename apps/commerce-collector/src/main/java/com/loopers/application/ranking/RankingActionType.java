package com.loopers.application.ranking;

import lombok.Getter;

/**
 * 랭킹 업데이트 액션 타입
 *
 */
@Getter
public enum RankingActionType {

    /**
     * 좋아요 추가 액션
     * 점수: +0.2점
     */
    LIKE_ADDED("좋아요 추가", 0.2),

    /**
     * 좋아요 취소 액션
     * 점수: -0.2점
     */
    LIKE_REMOVED("좋아요 취소", -0.2);

    // Enum 안에 추가 정보 저장
    private final String description;  // 설명
    private final double scoreChange;  // 해당 액션의 점수 변화량

    // 생성자
    RankingActionType(String description, double scoreChange) {
        this.description = description;
        this.scoreChange = scoreChange;
    }

}
