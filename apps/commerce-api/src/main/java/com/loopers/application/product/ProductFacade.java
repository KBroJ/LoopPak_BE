package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(Long brandId, String name, String description, long price, int stock, int maxOrderQuantity, ProductStatus
            status) {
        // 도메인 객체 생성을 위임
        Product product = Product.of(brandId, name, description, price, stock, maxOrderQuantity, status);
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

}
