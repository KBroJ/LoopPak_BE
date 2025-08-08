package com.loopers.infrastructure.points;

import com.loopers.domain.points.Point;
import com.loopers.domain.users.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<Point, Long> {

    @Query("SELECT p FROM Point p JOIN FETCH p.user WHERE p.user = :user")
    Optional<Point> findByUserWithUser(@Param("user") User user);

    Optional<Point> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Point p where p.user.id = :userId")
    Optional<Point> findByUserIdWithLock(@Param("userId") Long userId);

}
