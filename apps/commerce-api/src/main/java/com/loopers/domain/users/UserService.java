package com.loopers.domain.users;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserModel saveUser(String userId, String gender, String birthDate, String email) {

        UserModel userModel = UserModel.of(userId, gender, birthDate, email);

        return userRepository.save(userModel);
    }

}
