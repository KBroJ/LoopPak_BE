package com.loopers.domain.product;

import org.springframework.data.jpa.domain.Specification;

public class ProductSpecs {

    public static Specification<Product> isActive() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> isBrand(Long brandId) {

        if (brandId == null) {
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("brandId"), brandId);
    }

}
