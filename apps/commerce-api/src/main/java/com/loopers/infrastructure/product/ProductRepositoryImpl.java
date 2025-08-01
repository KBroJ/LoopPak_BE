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

    // ProductRepository의 새로운 search 메서드를 구현합니다.
    @Override
    public Page<Product> productList(Specification<Product> spec, Pageable pageable) {
        // 실제 쿼리 실행은 JpaSpecificationExecutor가 제공하는 findAll 메서드에 위임합니다.
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


}
