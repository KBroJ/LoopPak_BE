package com.loopers.application.order;

import com.loopers.application.points.PointApplicationService;
import com.loopers.domain.order.*;
import com.loopers.domain.points.Point;
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
    private final PointApplicationService pointApplicationService;

    @Transactional
    public Order placeOrder(Long userId, OrderRequest orderRequest) {

        List<Long> productIds = orderRequest.items().stream()
                .map(OrderItemRequest::productId)
                .toList();

        List<Product> products = productService.findProductsByIds(productIds);
        if (productIds.size() != products.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품 정보를 찾을 수 없습니다.");
        }

        Point userPoint = pointApplicationService.getPointByUserId(userId);

        Map<Long, Integer> quantityMap = orderRequest.items().stream()
                .collect(Collectors.toMap(OrderItemRequest::productId, OrderItemRequest::quantity));

        return orderService.placeOrder(userId, userPoint, products, quantityMap);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orderPage = orderService.findByUserId(userId, pageable);

        return orderPage.map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId) {
        Order order = orderService.findByIdWithItems(orderId);

        List<Long> productIds = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .toList();

        Map<Long, Product> productMap = productService.findProductsByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        return OrderDetailResponse.of(order, productMap);
    }

}
