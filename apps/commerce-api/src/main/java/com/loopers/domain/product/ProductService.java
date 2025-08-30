package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> productList(Long brandId, String sort, int page, int size) {

        if ("likes_desc".equals(sort)) {
            Pageable pageable = PageRequest.of(page, size);
            return productRepository.findActiveProductsOrderByLikesDesc(brandId, pageable);
        }

        Sort sortCondition = switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sortCondition);

        Specification<Product> spec = Specification.where(ProductSpecs.isActive());

        if (brandId != null) {
            spec = spec.and(ProductSpecs.isBrand(brandId));
        }

        return productRepository.productList(spec, pageable);
    }

    public Optional<Product> productInfo(Long productId) {
        return productRepository.productInfo(productId);
    }

    @Transactional
    public void increaseLikeCount(Long productId) {
        Product product = productRepository.productInfo(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.increaseLikeCount();
        productRepository.save(product);
    }

    @Transactional
    public void decreaseLikeCount(Long productId) {
        Product product = productRepository.productInfo(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.decreaseLikeCount();
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> findProductsByIds(List<Long> productIds, Pageable pageable) {
        if (productIds == null || productIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return productRepository.findByIdIn(productIds, pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findAllById(productIds);
    }

}
