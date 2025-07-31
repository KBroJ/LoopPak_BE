package com.loopers.domain.product;

import org.springframework.data.jpa.domain.Specification;

public class ProductSpecs {

    /**
     * "활성화(ACTIVE) 상태인가?" 라는 사양(Specification)
     */
    public static Specification<Product> isActive() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    /**
     * "특정 브랜드에 속하는가?" 라는 사양
     * brandId가 null이면 이 조건은 무시
     */
    public static Specification<Product> isBrand(Long brandId) {

        if (brandId == null) {
            return null; // null을 반환하면 and 연산 시 무시
        }

        // Product 엔티티의 'brandId' 필드가 파라미터로 받은 brandId와 같은지 비교하는 조건을 생성
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("brandId"), brandId);
    }

}
