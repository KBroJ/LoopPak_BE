package com.loopers.infrastructure.points;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.users.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<PointModel> findById(Long Id) {

        return pointJpaRepository.findById(Id);

    }

    @Override
    public PointModel save(PointModel pointModel) {
        return pointJpaRepository.save(pointModel);
    }

    @Override
    public Optional<PointModel> findByUserModelWithUser(UserModel user) {
        return pointJpaRepository.findByUserModelWithUser(user);
    }

}
