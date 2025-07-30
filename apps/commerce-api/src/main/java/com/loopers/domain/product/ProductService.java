package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> productList() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }
}
