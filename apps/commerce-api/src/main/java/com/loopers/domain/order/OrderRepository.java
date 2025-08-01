package com.loopers.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Optional<Order> findByIdWithItems(Long orderId);
}
