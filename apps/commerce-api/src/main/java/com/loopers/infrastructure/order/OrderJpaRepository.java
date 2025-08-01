package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    // userId를 기준으로 주문을 페이징하여 조회하는 기능
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // 주문 조회 시, 연관된 orderItems를 함께 fetch join하여 조회
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
}
