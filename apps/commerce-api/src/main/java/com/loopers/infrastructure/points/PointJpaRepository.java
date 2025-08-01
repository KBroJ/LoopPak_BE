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

    // PointModel의 필드인 userModel 객체의 id 필드를 기준으로 조회하는 메서드 추가
    Optional<PointModel> findByUserModel_Id(Long userId);

}
