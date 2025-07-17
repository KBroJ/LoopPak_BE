package com.loopers.domain.points;


import java.util.Optional;

public interface PointRepository {

    Optional<PointModel> findById(Long Id);

}
