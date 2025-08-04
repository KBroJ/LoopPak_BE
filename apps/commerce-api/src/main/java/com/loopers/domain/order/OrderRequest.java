package com.loopers.domain.order;

import java.util.List;

// 서비스가 사용할 요청 객체
public record OrderRequest(
    List<OrderItemRequest> items
) {
}
