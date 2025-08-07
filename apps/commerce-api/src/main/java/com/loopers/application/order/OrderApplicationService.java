package com.loopers.application.order;

import com.loopers.domain.order.*;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PointRepository pointRepository;

    @Transactional
    public Order placeOrder(Long userId, OrderRequest orderRequest) {
        // === 1. 데이터 조회 및 잠금(Lock) ===
        // Note: 동시성 제어를 위해 향후 이 부분에 락을 걸게 됩니다.
        List<Long> productIds = orderRequest.items().stream().map(OrderItemRequest::productId).toList();
        List<Product> products = productRepository.findAllById(productIds); // lock 필요
        Point userPoint = pointRepository.findByUserId(userId) // lock 필요
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 포인트 정보를 찾을 수 없습니다."));

        // === 2. 비즈니스 규칙 검증 ===
        // 요청된 상품이 모두 DB에 존재하는지 확인
        if (productIds.size() != products.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 상품 정보를 찾을 수 없습니다.");
        }

        Map<Long, Integer> quantityMap = orderRequest.items().stream()
                .collect(Collectors.toMap(OrderItemRequest::productId, OrderItemRequest::quantity));

        // 주문 아이템 생성 (재고 확인은 이 과정에서 자동으로 수행됨)
        List<OrderItem> orderItems = products.stream()
                .map(product -> {
                    int quantity = quantityMap.get(product.getId());
                    // Product Entity가 스스로 재고를 차감하고, 불가능하면 예외를 던짐
                    product.decreaseStock(quantity);
                    return OrderItem.of(product.getId(), quantity, product.getPrice());
                })
                .toList();

        // 총 주문 금액 계산 및 포인트 확인
        Order tempOrder = Order.of(userId, orderItems);
        long totalPrice = tempOrder.calculateTotalPrice();
        userPoint.use(totalPrice); // Point Entity가 스스로 포인트를 사용하고, 불가능하면 예외를 던짐

        // === 3. 상태 변경 및 영속화 ===
        return orderRepository.save(tempOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        // DTO 변환 로직은 여기에 직접 두거나, 별도 Mapper 클래스로 분리할 수 있습니다.
        return orderPage.map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문 정보를 찾을 수 없습니다."));

        List<Long> productIds = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .toList();

        // Product 정보를 Map 형태로 한번에 조회
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        return OrderDetailResponse.of(order, productMap);
    }

}
