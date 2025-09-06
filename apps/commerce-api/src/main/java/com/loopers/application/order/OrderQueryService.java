package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.users.User;
import com.loopers.domain.users.UserRepository;
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
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(String userId, int page, int size) {
        // User 조회 (String userId -> Long ID 변환)
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
        Long userInternalId = user.getId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByUserId(userInternalId, pageable);
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
