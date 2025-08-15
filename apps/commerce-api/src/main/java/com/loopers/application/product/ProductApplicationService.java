package com.loopers.application.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSpecs;
import com.loopers.domain.product.ProductStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductApplicationService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProductResponse create(Long brandId, String name, String description, long price, int stock, int maxOrderQuantity, ProductStatus
            status) {
        // 도메인 객체 생성을 위임
        Product product = Product.of(brandId, name, description, price, stock, maxOrderQuantity, status);
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(Long brandId, String sort, int page, int size) {

        // 1. 조회 조건에 따라 동적인 캐시 키 생성
        String cacheKey = "products:list::b" + brandId + ":s" + sort + ":p" + page + ":s" + size;

        // 2. 캐시에서 먼저 조회
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            // 3. 역직렬화 발생 : objectMapper.convertValue(cachedData, new TypeReference<>() {})
            //      => cachedData(JSON 문자열)를 Java 객체(PageResponse<ProductResponse>)로 다시 변환
            System.out.println("✅ Cache Hit! key: " + cacheKey);
            return objectMapper.convertValue(cachedData, new TypeReference<>() {});
        }

        System.out.println("🚨 Cache Miss! key: " + cacheKey);

        // 4. 캐시에 없으면 DB에서 조회
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
        PageResponse<ProductResponse> responseDto = PageResponse.from(productPage.map(ProductResponse::from));

        // 5. DB에서 가져온 데이터를 캐시에 저장 (유효시간 1분 설정)
        // 직렬화 발생 : RedisConfig에 설정해 둔 'GenericJackson2JsonRedisSerializer' 이 responseDto를 'JSON 문자열' 로 분해
        redisTemplate.opsForValue().set(cacheKey, responseDto, Duration.ofMinutes(1));

        return responseDto;
    }

    // ID 목록 순서에 맞게 Product 리스트를 정렬하고 Page 객체로 재구성하는 헬퍼 메소드
    private Page<Product> reorderProductsAndCreatePage(List<Product> products, List<Long> sortedIds, Page<Long> idPage) {
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<Product> sortedProducts = sortedIds.stream()
                .map(productMap::get)
                .collect(Collectors.toList());
        return new PageImpl<>(sortedProducts, idPage.getPageable(), idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId) {

        //  ----- Cache-Aside 로직 시작 -----
        String cacheKey = "product:detail:" + productId;

        // 1. 캐시에서 먼저 조회
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            System.out.println("✅ Cache Hit! productId: " + productId);
            return (ProductResponse) cachedData;
        }

        // 2. 캐시에 없으면(Cache Miss) DB에서 조회
        System.out.println("🚨 Cache Miss! productId: " + productId);
        Product product = productRepository.productInfo(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보를 찾을 수 없습니다."));

        ProductResponse response = ProductResponse.from(product);

        // 3. DB에서 가져온 데이터를 캐시에 저장 (유효시간 10분 설정)
        // Duration.ofMinutes(10) : TTL(Time To Live) 설정 - 이 데이터는 캐시에 저장된 후 10분이 지나면 자동으로 삭제
        redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(10));
        //  ----- Cache-Aside 로직 종료 -----

        return response;

    }

}
