package com.loopers.domain.points;


import com.loopers.domain.users.UserModel;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointRepository {

    Optional<PointModel> findById(Long Id);

    PointModel save(PointModel pointModel);

    Optional<PointModel> findByUserModelWithUser(@Param("user") UserModel user);

    // User PK로 Point를 찾는 메서드 시그니처 추가
    Optional<PointModel> findByUserId(Long userId);
}
