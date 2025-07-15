package com.loopers.domain.users;

import java.util.Optional;

public interface UserRepository {

    UserModel save(UserModel userModel);

    boolean existsByUserId(String userId);

    Optional<UserModel> findByUserId(String userId);

}
