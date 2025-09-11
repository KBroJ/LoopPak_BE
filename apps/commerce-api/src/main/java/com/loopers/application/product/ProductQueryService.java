package com.loopers.application.product;

import com.loopers.domain.product.*;
import com.loopers.domain.ranking.RankingRepository;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final RankingRepository rankingRepository;
    private final ProductCacheRepository productCacheRepository;

    // 날짜 형식 상수 추가
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

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

        // 1. 캐시에서 먼저 조회 (기존 로직 그대로)
        Optional<ProductResponse> cached = productCacheRepository.getProductDetail(productId);
        if (cached.isPresent()) {
            ProductResponse cachedResponse = cached.get();

            // 캐시된 데이터에 최신 랭킹 정보 추가
            return addRankingInfo(cachedResponse, productId);
        }

        // 2. 캐시에 없으면 DB에서 조회
        Product product = productRepository.productInfo(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보를 찾을 수 없습니다."));

        // 3. 랭킹 정보 조회
        String today = LocalDate.now().format(DATE_FORMAT);
        Integer currentRank = rankingRepository.getProductRank(today, productId);
        Double currentScore = rankingRepository.getProductScore(today, productId);

        // 4. 랭킹 정보가 포함된 응답 생성
        ProductResponse response = ProductResponse.withRanking(product, currentRank, currentScore);

        // 5. 랭킹 정보 없는 기본 응답을 캐시에 저장 (랭킹은 실시간 변동이므로 캐시 제외)
        ProductResponse basicResponse = ProductResponse.from(product);
        productCacheRepository.saveProductDetail(productId, basicResponse);

        return response;
    }

    /**
     * 캐시된 ProductResponse에 최신 랭킹 정보 추가
     *
     * @param cachedResponse 캐시된 응답 (랭킹 정보 없음)
     * @param productId 상품 ID
     * @return 랭킹 정보가 추가된 응답
     */
    private ProductResponse addRankingInfo(ProductResponse cachedResponse, Long productId) {
        String today = LocalDate.now().format(DATE_FORMAT);
        Integer currentRank = rankingRepository.getProductRank(today, productId);
        Double currentScore = rankingRepository.getProductScore(today, productId);

        // 기존 캐시 데이터에 랭킹 정보만 추가
        return new ProductResponse(
                cachedResponse.productId(),
                cachedResponse.brandId(),
                cachedResponse.name(),
                cachedResponse.description(),
                cachedResponse.price(),
                cachedResponse.stock(),
                cachedResponse.productStatus(),
                cachedResponse.likeCount(),
                currentRank,   // 새로운 랭킹 정보
                currentScore   // 새로운 점수 정보
        );
    }

}
