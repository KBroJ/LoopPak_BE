package com.loopers.infrastructure.points;

import com.loopers.domain.points.PointModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<PointModel, Long> {
}
