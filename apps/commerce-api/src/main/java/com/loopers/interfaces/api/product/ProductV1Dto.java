package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductResponse;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;

public class ProductV1Dto {

    public record Summary(
            Long id,
            String name,
            long price,
            Long brandId,
            long likeCount
    ) {
        public static Summary from(ProductResponse response) {
            return new Summary(
                    response.getProduct().getId(),
                    response.getProduct().getName(),
                    response.getProduct().getPrice(),
                    response.getProduct().getBrandId(),
                    response.getLikeCount()
            );
        }
    }

    public record Detail(
            Long id,
            String name,
            String description,
            long price,
            int stock,
            ProductStatus status,
            Long brandId,
            long likeCount
    ) {
        public static Detail from(ProductResponse response) {
            Product product = response.getProduct();
            return new Detail(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStock(),
                    product.getStatus(),
                    product.getBrandId(),
                    response.getLikeCount()
            );
        }
    }

}
