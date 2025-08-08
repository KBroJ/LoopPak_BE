package com.loopers.domain.points;


import com.loopers.domain.users.User;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointRepository {

    Optional<Point> findById(Long Id);

    Point save(Point point);

    Optional<Point> findByUserWithUser(@Param("user") User user);

    Optional<Point> findByUserId(Long userId);

    // 동시성 제어를 위한 조회 메소드 추가 (비관적 락)
    Optional<Point> findByUserIdWithLock(Long userId);
}
