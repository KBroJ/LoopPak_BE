package com.loopers.infrastructure.points;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.users.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointModel, Long> {

    @Query("SELECT p FROM PointModel p JOIN FETCH p.userModel WHERE p.userModel = :user")
    Optional<PointModel> findByUserModelWithUser(@Param("user") UserModel user);

    Optional<PointModel> findByUserModel_Id(Long userId);

}
