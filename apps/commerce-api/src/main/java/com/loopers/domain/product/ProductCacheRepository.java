package com.loopers.domain.product;

import com.loopers.application.product.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * 상품 캐시 저장소 인터페이스
 * Domain 계층에서 캐시 관련 요구사항을 정의하고,
 * Infrastructure 계층에서 구체적인 캐시 기술(Redis 등)을 구현한다.
 */
public interface ProductCacheRepository {

    /**
     * 상품 목록 캐시 조회
     * @param brandId 브랜드 ID (null 가능)
     * @param sort 정렬 조건
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 캐시된 상품 목록 (없으면 Optional.empty())
     */
    Optional<Page<ProductResponse>> getProductList(
        Long brandId, String sort, int page, int size
    );

    /**
     * 상품 목록 캐시 저장
     * @param brandId 브랜드 ID (null 가능)
     * @param sort 정렬 조건
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param productPage 저장할 상품 목록 데이터
     */
    void saveProductList(
        Long brandId, String sort, int page, int size,
        Page<ProductResponse> productPage
    );
    
    /**
     * 상품 상세 캐시 조회
     * @param productId 상품 ID
     * @return 캐시된 상품 상세 정보 (없으면 Optional.empty())
     */
    Optional<ProductResponse> getProductDetail(Long productId);
    
    /**
     * 상품 상세 캐시 저장
     * @param productId 상품 ID
     * @param productResponse 저장할 상품 상세 데이터
     */
    void saveProductDetail(Long productId, ProductResponse productResponse);
    
    /**
     * 특정 브랜드의 상품 관련 캐시 무효화
     * (상품 데이터 변경 시 관련 캐시를 삭제하기 위함)
     * @param brandId 브랜드 ID
     */
    void evictProductsByBrand(Long brandId);
    
    /**
     * 특정 상품의 상세 캐시 무효화
     * @param productId 상품 ID
     */
    void evictProductDetail(Long productId);
}