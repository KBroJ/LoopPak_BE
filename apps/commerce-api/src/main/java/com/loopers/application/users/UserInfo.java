package com.loopers.application.users;

import com.loopers.domain.users.UserModel;

import java.time.LocalDate;

public record UserInfo(
        Long id,
        String userId, String gender, LocalDate birthDate, String email
) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
            model.getId(),
            model.getUserId(),
            model.getGender(),
            model.getBirthDate(),
            model.getEmail()
        );
    }
}
