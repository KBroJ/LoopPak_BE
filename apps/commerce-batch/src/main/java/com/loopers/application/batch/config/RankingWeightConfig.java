package com.loopers.application.batch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 랭킹 가중치 설정 관리
 * application.yml의 ranking.weight 하위 설정값들을 바인딩
 */
@Data                                                   // Lombok으로 getter/setter 자동 생성
@Component                                              // Spring Bean으로 등록
@ConfigurationProperties(prefix = "ranking.weight")     // application.yml의 ranking.weight.* 값들을 자동 바인딩
public class RankingWeightConfig {

    /**
     * 좋아요 가중치 (기본값: 3.0)
     * 사용자의 관심도를 가장 높게 평가
     */
    private double like = 3.0;

    /**
     * 판매량 가중치 (기본값: 2.0)
     * 실제 구매로 이어진 상품의 가치 평가
     */
    private double sales = 2.0;

    /**
     * 조회수 가중치 (기본값: 1.0)
     * 기본적인 관심도 지표
     */
    private double view = 1.0;

}
