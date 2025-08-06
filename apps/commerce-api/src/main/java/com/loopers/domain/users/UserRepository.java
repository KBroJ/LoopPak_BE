package com.loopers.domain.users;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    boolean existsByUserId(String userId);

    Optional<User> findByUserId(String userId);

}
