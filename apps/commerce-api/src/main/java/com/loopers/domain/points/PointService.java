package com.loopers.domain.points;

import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointService {

    private final UserService userService;
    private final PointRepository pointRepository;

    public PointModel getPointInfo(String userId) {

        UserModel userInfo = userService.getMyInfo(userId);
        if (userInfo == null) {
            return null;
        }

        PointModel pointInfo = pointRepository.findById(userInfo.getId())
                .orElse(null);

        return pointInfo;

    }


}
