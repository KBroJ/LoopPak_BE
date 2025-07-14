package com.loopers.interfaces.api.users;

import com.loopers.application.example.ExampleInfo;
import com.loopers.application.users.UserInfo;
import com.loopers.interfaces.api.example.ExampleV1Dto;

import java.time.LocalDate;

public class UsersV1Dto {

    public record UsersSaveRequest(
        String userId, String gender, String birthDate, String email
    ) {
    }

    public record UsersResponse(
            Long id,
            String userId, String gender, LocalDate birthDate, String email
    ) {
        public static UsersV1Dto.UsersResponse from(UserInfo info) {
            return new UsersV1Dto.UsersResponse(
                    info.id(),
                    info.userId(),
                    info.gender(),
                    info.birthDate(),
                    info.email()
            );
        }
    }

}
