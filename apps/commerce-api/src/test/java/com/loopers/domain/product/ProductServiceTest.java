package com.loopers.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("기본 정렬(latest)로 목록 조회 시, productList 메서드를 호출한다.")
    void productList_callsProductList_forDefaultSort() {
        // given
        given(productRepository.productList(any(Specification.class), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        productService.productList(null, "latest", 0, 10);

        // then
        verify(productRepository).productList(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("'좋아요 순' 정렬로 목록 조회 시, findActiveProductsOrderByLikesDesc 메서드를 호출한다.")
    void productList_callsOrderByLikesDesc_forLikesDescSort() {
        // given
        given(productRepository.findActiveProductsOrderByLikesDesc(any(), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        productService.productList(null, "likes_desc", 0, 10);

        // then
        verify(productRepository).findActiveProductsOrderByLikesDesc(any(), any(Pageable.class));
    }

}
