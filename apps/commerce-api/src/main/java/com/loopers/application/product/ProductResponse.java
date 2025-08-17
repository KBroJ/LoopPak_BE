package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;

public record ProductResponse(
        Long productId,
        Long brandId,
        String name,
        String description,
        long price,
        int stock,
        ProductStatus productStatus,
        long likeCount

) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getBrandId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getLikeCount()
        );
    }

}
