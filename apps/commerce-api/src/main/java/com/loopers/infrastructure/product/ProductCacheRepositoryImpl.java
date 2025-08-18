package com.loopers.infrastructure.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductResponse;
import com.loopers.domain.product.ProductCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis를 사용한 상품 캐시 저장소 구현체
 * StringRedisTemplate을 사용하여 타입 정보 없이 순수 JSON으로 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheRepositoryImpl implements ProductCacheRepository {
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 캐시 키 네임스페이스
    private static final String PRODUCT_LIST_PREFIX = "products:list";
    private static final String PRODUCT_DETAIL_PREFIX = "product:detail";
    
    // TTL 설정
    private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(1);
    private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(10);

    /**
     *  상품 목록 캐시 조회
     */
    @Override
    public Optional<Page<ProductResponse>> getProductList(
            Long brandId, String sort, int page, int size
    ) {
        
        try {
            String cacheKey = buildProductListKey(brandId, sort, page, size);
            String json = redisTemplate.opsForValue().get(cacheKey);
            
            if (json == null) {
                log.debug("🚨 Cache Miss! key: {}", cacheKey);
                return Optional.empty();
            }
            
            log.debug("✅ Cache Hit! key: {}", cacheKey);
            Page<ProductResponse> result = objectMapper.readValue(
                json, new TypeReference<Page<ProductResponse>>() {}
            );
            
            return Optional.of(result);
            
        } catch (Exception e) {
            log.warn("캐시 조회 실패 (Cache Miss로 처리): {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void saveProductList(
            Long brandId, String sort, int page, int size,
            Page<ProductResponse> productPage
    ) {
        
        if ( productPage == null || productPage.getContent().isEmpty()) {
            return; // 빈 데이터는 캐시하지 않음
        }
        
        try {
            String cacheKey = buildProductListKey(brandId, sort, page, size);
            String json = objectMapper.writeValueAsString(productPage);
            
            Duration ttlWithJitter = addJitter(PRODUCT_LIST_TTL);
            redisTemplate.opsForValue().set(cacheKey, json, ttlWithJitter);
            
            log.debug("캐시 저장 완료. key: {}, ttl: {}초", cacheKey, ttlWithJitter.getSeconds());
            
        } catch (Exception e) {
            log.warn("캐시 저장 실패 (무시하고 계속 진행): {}", e.getMessage());
        }
    }
    
    @Override
    public Optional<ProductResponse> getProductDetail(Long productId) {
        
        try {
            String cacheKey = buildProductDetailKey(productId);
            String json = redisTemplate.opsForValue().get(cacheKey);
            
            if (json == null) {
                log.debug("🚨 Cache Miss! productId: {}", productId);
                return Optional.empty();
            }
            
            log.debug("✅ Cache Hit! productId: {}", productId);
            ProductResponse result = objectMapper.readValue(json, ProductResponse.class);
            
            return Optional.of(result);
            
        } catch (Exception e) {
            log.warn("캐시 조회 실패 (Cache Miss로 처리): {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public void saveProductDetail(Long productId, ProductResponse productResponse) {
        
        if (productResponse == null) {
            return;
        }
        
        try {
            String cacheKey = buildProductDetailKey(productId);
            String json = objectMapper.writeValueAsString(productResponse);
            
            Duration ttlWithJitter = addJitter(PRODUCT_DETAIL_TTL);
            redisTemplate.opsForValue().set(cacheKey, json, ttlWithJitter);
            
            log.debug("캐시 저장 완료. key: {}, ttl: {}초", cacheKey, ttlWithJitter.getSeconds());
            
        } catch (Exception e) {
            log.warn("캐시 저장 실패 (무시하고 계속 진행): {}", e.getMessage());
        }
    }
    
    @Override
    public void evictProductsByBrand(Long brandId) {
        
        try {
            String pattern = PRODUCT_LIST_PREFIX + "::b" + brandId + ":*";
            log.debug("브랜드 캐시 무효화 요청: {}", pattern);
            
        } catch (Exception e) {
            log.warn("캐시 무효화 실패 (무시하고 계속 진행): {}", e.getMessage());
        }
    }
    
    @Override
    public void evictProductDetail(Long productId) {
        
        try {
            String cacheKey = buildProductDetailKey(productId);
            redisTemplate.delete(cacheKey);
            log.debug("상품 상세 캐시 삭제: {}", cacheKey);
            
        } catch (Exception e) {
            log.warn("캐시 삭제 실패 (무시하고 계속 진행): {}", e.getMessage());
        }
    }
    
    /**
     * 상품 목록 캐시 키 생성
     * 형식: "products:list::b{brandId}:s{sort}:p{page}:s{size}"
     */
    private String buildProductListKey(Long brandId, String sort, int page, int size) {
        return PRODUCT_LIST_PREFIX + "::b" + brandId + ":s" + sort + ":p" + page + ":s" + size;
    }
    
    /**
     * 상품 상세 캐시 키 생성  
     * 형식: "product:detail:{productId}"
     */
    private String buildProductDetailKey(Long productId) {
        return PRODUCT_DETAIL_PREFIX + ":" + productId;
    }
    
    /**
     * TTL에 랜덤 Jitter를 추가하여 Cache Stampede 방지
     * 기본 TTL에서 ±30초 랜덤하게 조정
     */
    private Duration addJitter(Duration baseTtl) {
        int jitterSeconds = ThreadLocalRandom.current().nextInt(-30, 31);
        return baseTtl.plusSeconds(jitterSeconds);
    }
}