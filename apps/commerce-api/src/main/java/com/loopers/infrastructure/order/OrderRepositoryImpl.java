package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Page<Order> findByUserId(Long userId, Pageable pageable) {
        return orderJpaRepository.findByUserId(userId, pageable);
    }

    @Override
    public Optional<Order> findByIdWithItems(Long orderId) {
        return orderJpaRepository.findByIdWithItems(orderId);
    }

}
