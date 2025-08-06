package com.loopers.infrastructure.points;

import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> findById(Long Id) {
        return pointJpaRepository.findById(Id);
    }

    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }

    @Override
    public Optional<Point> findByUserWithUser(User user) {
        return pointJpaRepository.findByUserWithUser(user);
    }

    @Override
    public Optional<Point> findByUserId(Long userId) {
        return pointJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Point> findByUserIdWithLock(Long userId) {
        return pointJpaRepository.findByUserIdWithLock(userId);
    }

}
