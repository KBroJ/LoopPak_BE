package com.loopers.infrastructure.users;

import com.loopers.domain.users.User;
import com.loopers.domain.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return userJpaRepository.existsByUserId(userId);
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        return userJpaRepository.findByUserId(userId);
    }
}
