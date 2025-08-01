package com.loopers.application.order;

import com.loopers.domain.order.*;
import com.loopers.domain.points.PointModel;
import com.loopers.domain.points.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final PointService pointService;

    /**
     *
     * @param userId
     * @param orderRequest
     * @return
     */
    @Transactional
    public Order placeOrder(Long userId, OrderRequest orderRequest) {

        // --- 1. 데이터 조회 및 준비 (조율) ---
        List<Long> productIds = orderRequest.items().stream()
                .map(OrderItemRequest::productId)
                .toList();

        // 상품 정보 조회
        List<Product> products = productService.findProductsByIds(productIds);
        if (productIds.size() != products.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품 정보를 찾을 수 없습니다.");
        }

        // 포인트 정보 조회
        PointModel userPoint = pointService.getPointByUserId(userId);

        // 상품 ID와 수량을 Map으로 변환하여 OrderService에 전달하기 쉽게 가공
        Map<Long, Integer> quantityMap = orderRequest.items().stream()
                .collect(Collectors.toMap(OrderItemRequest::productId, OrderItemRequest::quantity));

        // --- 2. 핵심 로직 위임 ---
        return orderService.placeOrder(userId, userPoint, products, quantityMap);
    }

    /**
     * 특정 사용자의 주문 목록을 페이징하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long userId, int page, int size) {
        // 1. 페이징 및 정렬 정보 생성 (최신순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 2. OrderRepository 대신 OrderService를 통해 주문 데이터를 조회합니다.
        Page<Order> orderPage = orderService.findByUserId(userId, pageable);

        // 3. Page<Order>를 Page<OrderSummaryResponse>로 변환하여 반환
        return orderPage.map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId) {
        // 1. OrderService를 통해 주문 정보와 주문 상품(OrderItem) 목록을 함께 조회
        Order order = orderService.findByIdWithItems(orderId);

        // 2. 주문 상품들로부터 상품 ID 목록을 추출
        List<Long> productIds = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .toList();

        // 3. ProductService를 통해 상품 이름 등 상세 정보를 한 번에 조회
        Map<Long, Product> productMap = productService.findProductsByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        // 4. 주문 정보와 상품 정보를 조합하여 최종 DTO를 생성 후 반환
        return OrderDetailResponse.of(order, productMap);
    }

}
