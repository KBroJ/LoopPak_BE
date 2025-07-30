package com.loopers.domain.product;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    List<Product> findByStatus(ProductStatus status);

}
