package com.loopers.application.batch.reader;

import com.loopers.domain.metrics.ProductMetrics;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.stereotype.Component;

/**
 * ProductMetrics 데이터를 읽어오는 Spring Batch Reader
 *
 * 역할:
 * - commerce-collector의 product_metrics 테이블에서 모든 상품의 누적 지표 조회
 * - 페이징 처리로 대량 데이터 안전하게 읽기
 * - Spring Batch의 Chunk-Oriented Processing에서 Reader 역할 담당
 *
 * 데이터 흐름:
 * product_metrics → ProductMetricsReader → Processor → Writer → MV 테이블
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsReader {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * ProductMetrics 전체 데이터를 페이징으로 읽는 ItemReader 생성
     *
     * @return JpaPagingItemReader<ProductMetrics>
     */
    public ItemReader<ProductMetrics> createReader() {
        log.info("ProductMetrics Reader 초기화 중...");

        return new JpaPagingItemReaderBuilder<ProductMetrics>()
                .name("productMetricsReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT pm FROM ProductMetrics pm ORDER BY pm.productId ASC")
                .pageSize(100) // 한 번에 100개씩 읽기
                .build();
    }

    /**
     * 특정 조건의 ProductMetrics를 읽는 Reader (필요시 사용)
     *
     * @param minLikeCount 최소 좋아요 수 (필터링용)
     * @return ItemReader
     */
    public ItemReader<ProductMetrics> createReaderWithFilter(int minLikeCount) {
        log.info("ProductMetrics Reader (필터: 최소 좋아요 {}개) 초기화 중...", minLikeCount);

        return new JpaPagingItemReaderBuilder<ProductMetrics>()
                .name("productMetricsReaderWithFilter")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT pm FROM ProductMetrics pm WHERE pm.likeCount >= :minLikeCount ORDER BY pm.productId ASC")
                                .parameterValues(java.util.Map.of("minLikeCount", minLikeCount))
                                .pageSize(100)
                                .build();
    }

    /**
     * 상위 N개 상품만 읽는 Reader (성능 최적화용)
     *
     * @param topN 상위 N개 (예: 1000개만 읽어서 TOP 100 선별)
     * @return ItemReader
     */
    public ItemReader<ProductMetrics> createTopNReader(int topN) {
        log.info("ProductMetrics Top {} Reader 초기화 중...", topN);

        // 점수 기반 정렬로 상위 N개만 읽기
        String jpql = """
                  SELECT pm FROM ProductMetrics pm
                  ORDER BY (pm.likeCount * 3.0 + pm.salesCount * 2.0 + pm.viewCount * 1.0) DESC
                  """;

        JpaPagingItemReader<ProductMetrics> reader = new JpaPagingItemReaderBuilder<ProductMetrics>()
                .name("productMetricsTopNReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpql)
                .pageSize(100)
                .maxItemCount(topN) // 최대 N개까지만 읽기
                .build();

        return reader;
    }

}
