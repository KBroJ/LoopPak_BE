package com.loopers.domain.order;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order placeOrder(Long userId, PointModel userPoint, List<Product> products, Map<Long, Integer> quantityMap) {

        // 1. 전달받은 Product 객체들로 OrderItem 리스트를 생성하면서 재고를 차감합니다.
        List<OrderItem> orderItems = products.stream()
                .map(product -> {
                    int quantity = quantityMap.get(product.getId());
                    product.decreaseStock(quantity); // 재고 차감
                    return OrderItem.of(product.getId(), quantity, product.getPrice());
                })
                .toList();

        // 2. Order 엔티티를 생성하고 총 주문 금액을 계산합니다.
        Order newOrder = Order.of(userId, orderItems);
        long totalPrice = newOrder.calculateTotalPrice();

        // 3. 전달받은 Point 객체에서 포인트를 차감합니다.
        userPoint.use(totalPrice);

        // 4. 주문을 저장하고 반환합니다.
        return orderRepository.save(newOrder);
    }

    /**
     * 특정 사용자의 주문 목록을 조회하는 기능을 추가합니다.
     */
    @Transactional(readOnly = true)
    public Page<Order> findByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * 단일 주문 상세 조회
     */
    @Transactional(readOnly = true)
    public Order findByIdWithItems(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문 정보를 찾을 수 없습니다."));
    }
}
