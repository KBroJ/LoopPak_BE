package com.loopers.application.product;

import com.loopers.domain.product.*;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductCacheRepository productCacheRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(Long brandId, String sort, int page, int size) {

        // 1. 캐시에서 먼저 조회
        Optional<Page<ProductResponse>> cached = productCacheRepository.getProductList(brandId, sort, page, size);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. 캐시에 없으면 DB에서 조회
        Sort sortCondition = switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "likes_desc" -> Sort.by(Sort.Direction.DESC, "likeCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sortCondition);
        Specification<Product> spec = Specification.where(ProductSpecs.isActive());
        if (brandId != null) {
            spec = spec.and(ProductSpecs.isBrand(brandId));
        }

        Page<Product> productPage = productRepository.productList(spec, pageable);
        Page<ProductResponse> responseDto = productPage.map(ProductResponse::from);

        // 3. DB에서 가져온 데이터를 캐시에 저장
        productCacheRepository.saveProductList(brandId, sort, page, size, responseDto);

        return responseDto;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId) {

        // 1. 캐시에서 먼저 조회
        Optional<ProductResponse> cached = productCacheRepository.getProductDetail(productId);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. 캐시에 없으면 DB에서 조회
        Product product = productRepository.productInfo(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보를 찾을 수 없습니다."));

        ProductResponse response = ProductResponse.from(product);

        // 3. DB에서 가져온 데이터를 캐시에 저장
        productCacheRepository.saveProductDetail(productId, response);

        return response;
    }

}
