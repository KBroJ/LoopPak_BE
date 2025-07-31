package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


public interface ProductRepository {

    Product save(Product product);

    Page<Product> search(Specification<Product> spec, Pageable pageable);

}
