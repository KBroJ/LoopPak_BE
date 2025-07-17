package com.loopers.application.users;

import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;

    public UserInfo saveUser(String userId, String gender, String birthDate,String email) {
        UserModel userModel = userService.saveUser(userId, gender, birthDate, email);

        return UserInfo.from(userModel);
    }

    public UserInfo getMyInfo(String userId) {

        UserModel userModel = userService.getMyInfo(userId);

        if (userModel == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }

        return UserInfo.from(userModel);

    }

}
