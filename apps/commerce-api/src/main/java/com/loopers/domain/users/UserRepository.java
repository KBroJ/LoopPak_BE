package com.loopers.domain.users;

public interface UserRepository {

    UserModel save(UserModel userModel);

    boolean existsByUserId(String userId);

}
