package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;


public interface ProductRepository {

    Product save(Product product);

    Page<Product> productList(Specification<Product> spec, Pageable pageable);

    Optional<Product> productInfo(Long productId);
}
