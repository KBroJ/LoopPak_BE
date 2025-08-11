package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Page<Product> productList(Specification<Product> spec, Pageable pageable) {
        return productJpaRepository.findAll(spec, pageable);
    }

    @Override
    public Optional<Product> productInfo(Long productId) {
        return productJpaRepository.findById(productId);
    }

    @Override
    public Page<Product> findByIdIn(List<Long> productIds, Pageable pageable) {
        return productJpaRepository.findByIdIn(productIds, pageable);
    }

    @Override
    public List<Product> findAllById(List<Long> productIds) {
        return productJpaRepository.findAllById(productIds);
    }

    @Override
    public List<Product> findAllByIdWithLock(List<Long> productIds) {
        return productJpaRepository.findAllByIdWithLock(productIds);
    }

    @Override
    public Page<Product> findActiveProductsOrderByLikesDesc(Long brandId, Pageable pageable) {
        return productJpaRepository.findActiveProductsOrderByLikesDesc(brandId, pageable);
    }

}
