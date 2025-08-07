package com.loopers.application.product;

import com.loopers.domain.like.LikeCountDto;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSpecs;
import com.loopers.domain.product.ProductStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductApplicationService {

    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;

    @Transactional
    public ProductResponse create(Long brandId, String name, String description, long price, int stock, int maxOrderQuantity, ProductStatus
            status) {
        // 도메인 객체 생성을 위임
        Product product = Product.of(brandId, name, description, price, stock, maxOrderQuantity, status);
        Product savedProduct = productRepository.save(product);
        // '좋아요'는 아직 없으므로 0으로 DTO 생성
        return ProductResponse.from(savedProduct, 0L);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(Long brandId, String sort, int page, int size) {

        Page<Product> productPage;

        // '좋아요순' 정렬은 처리 방식이 다르므로 분기합니다.
        if ("likes_desc".equals(sort)) {
            // 1. '좋아요' 많은 순으로 정렬된 상품 ID 목록을 페이지 단위로 가져옵니다.
            Pageable idPageable = PageRequest.of(page, size);
            Page<Long> topLikedProductIdsPage = likeRepository.findProductIdsOrderByLikesDesc(brandId, idPageable);

            // 2. 해당 ID 목록으로 Product 리스트를 조회합니다.
            List<Long> productIds = topLikedProductIdsPage.getContent();
            List<Product> products = productRepository.findAllById(productIds);

            // 3. ID 순서에 맞게 Product 리스트를 재정렬하고 Page 객체로 만듭니다.
            productPage = reorderProductsAndCreatePage(products, productIds, topLikedProductIdsPage);

        } else {
            // 그 외 정렬 (최신순, 가격순 등 DB에서 직접 처리)
            Sort sortCondition = switch (sort) {
                case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
            Pageable pageable = PageRequest.of(page, size, sortCondition);
            Specification<Product> spec = Specification.where(ProductSpecs.isActive());
            if (brandId != null) {
                spec = spec.and(ProductSpecs.isBrand(brandId));
            }
            productPage = productRepository.productList(spec, pageable);
        }

        // 최종적으로 Product 페이지에 '좋아요' 수를 매핑하여 DTO로 변환합니다.
        return mapLikeCountsToResponsePage(productPage);
    }

    // ID 목록 순서에 맞게 Product 리스트를 정렬하고 Page 객체로 재구성하는 헬퍼 메소드
    private Page<Product> reorderProductsAndCreatePage(List<Product> products, List<Long> sortedIds, Page<Long> idPage) {
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<Product> sortedProducts = sortedIds.stream()
                .map(productMap::get)
                .collect(Collectors.toList());
        return new PageImpl<>(sortedProducts, idPage.getPageable(), idPage.getTotalElements());
    }

    // Product 페이지에 '좋아요' 수를 매핑하여 최종 DTO 페이지를 만드는 헬퍼 메소드
    private Page<ProductResponse> mapLikeCountsToResponsePage(Page<Product> productPage) {
        if (productPage.isEmpty()) {
            return Page.empty();
        }

        List<Long> productIds = productPage.getContent().stream().map(Product::getId).toList();
        List<LikeCountDto> likeCountDtos = likeRepository.countByTargetIdIn(productIds, LikeType.PRODUCT);

        Map<Long, Long> likeCounts = likeCountDtos.stream()
                .collect(Collectors.toMap(LikeCountDto::targetId, LikeCountDto::count));

        return productPage.map(product -> ProductResponse.from(product, likeCounts.getOrDefault(product.getId(), 0L)));
    }


    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId) {
        Product product = productRepository.productInfo(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보를 찾을 수 없습니다."));

        long likeCount = likeRepository.getLikeCount(productId);

        return ProductResponse.from(product, likeCount);
    }

}
