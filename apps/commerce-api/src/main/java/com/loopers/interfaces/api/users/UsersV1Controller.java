package com.loopers.interfaces.api.users;

import com.loopers.application.users.UserApplicationService;
import com.loopers.application.users.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UsersV1Controller implements UsersV1ApiSpec {

    private final UserApplicationService userApplicationService;

    @PostMapping
    @Override
    public ApiResponse<UsersV1Dto.UsersResponse> saveUser(
        @RequestBody UsersV1Dto.UsersSaveRequest request
    ) {
        UserInfo info = userApplicationService.saveUser(
            request.userId(), request.gender(), request.birthDate(), request.email()
        );

        UsersV1Dto.UsersResponse response = UsersV1Dto.UsersResponse.from(info);

        return ApiResponse.success(response);
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UsersV1Dto.UsersResponse> getMyInfo(
        @RequestHeader("X-USER-ID") String userId
    ) {
        UserInfo info = userApplicationService.getMyInfo(userId);

        UsersV1Dto.UsersResponse response = UsersV1Dto.UsersResponse.from(info);

        return ApiResponse.success(response);
    }


}
