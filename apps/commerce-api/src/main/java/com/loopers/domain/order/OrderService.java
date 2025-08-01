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

        List<OrderItem> orderItems = products.stream()
                .map(product -> {
                    int quantity = quantityMap.get(product.getId());
                    product.decreaseStock(quantity);
                    return OrderItem.of(product.getId(), quantity, product.getPrice());
                })
                .toList();

        Order newOrder = Order.of(userId, orderItems);
        long totalPrice = newOrder.calculateTotalPrice();

        userPoint.use(totalPrice);

        return orderRepository.save(newOrder);
    }

    @Transactional(readOnly = true)
    public Page<Order> findByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Order findByIdWithItems(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문 정보를 찾을 수 없습니다."));
    }
}
