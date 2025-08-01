package com.loopers.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 상품 목록 조회 : 상품 목록을 동적으로 검색합니다.
     * @return 페이징 및 정렬이 적용된 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<Product> productList(Long brandId, String sort, int page, int size) {

        // 1. 정렬(Sort) 조건 생성
        Sort sortCondition = switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // 기본값: 최신순
        };

        // 2. 페이징(Pageable) 정보 생성 (페이지, 사이즈, 정렬 포함)
        Pageable pageable = PageRequest.of(page, size, sortCondition);

        // 3. Specification(검색 조건) 조합
        Specification<Product> spec = Specification.where(ProductSpecs.isActive());

        if (brandId != null) {
            spec = spec.and(ProductSpecs.isBrand(brandId));
        }

        // 4. Repository 호출
        return productRepository.productList(spec, pageable);
    }

    /**
     * 상품 정보 조회
     */
    public Optional<Product> productInfo(Long productId) {
        return productRepository.productInfo(productId);
    }

    /**
     * 내가 좋아요 한 상품 목록 조회 : 좋아요 한 상품 ID 목록으로 상품 정보를 페이징하여 조회합니다.
     * @return 페이징 처리된 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<Product> findProductsByIds(List<Long> productIds, Pageable pageable) {
        // ID 목록이 비어있으면 굳이 DB를 조회할 필요 없이 빈 페이지를 반환합니다.
        if (productIds == null || productIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return productRepository.findByIdIn(productIds, pageable);
    }

}
