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
                    response.productId(),
                    response.name(),
                    response.price(),
                    response.brandId(),
                    response.likeCount()
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
            return new Detail(
                    response.productId(),
                    response.name(),
                    response.description(),
                    response.price(),
                    response.stock(),
                    response.productStatus(),
                    response.brandId(),
                    response.likeCount()
            );
        }
    }

}
