package com.loopers.interfaces.api.like;

public class LikeV1Dto {

    public record Product(
            Long id,
            String name,
            long price,
            Long brandId
    ) {
        public static Product from(com.loopers.domain.product.Product product) {
            return new Product(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getBrandId()
            );
        }
    }

}
