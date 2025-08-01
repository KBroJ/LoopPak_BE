package com.loopers.application.product;

import com.loopers.domain.product.Product;
import lombok.Getter;

@Getter
public class ProductResponse {

    private final Product product;
    private final long likeCount;

    public ProductResponse(Product product, long likeCount) {
        this.product = product;
        this.likeCount = likeCount;
    }

}
