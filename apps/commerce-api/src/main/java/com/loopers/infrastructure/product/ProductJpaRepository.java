package com.loopers.infrastructure.product;


import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(ProductStatus status);
}
