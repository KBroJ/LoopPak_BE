package com.loopers.infrastructure.users;

import com.loopers.domain.users.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserModel, Long> {
    boolean existsByUserId(String userId);
    Optional<UserModel> findByUserId(String userId);
}
