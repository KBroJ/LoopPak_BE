package com.loopers.domain.points;

import com.loopers.domain.users.UserModel;
import com.loopers.domain.users.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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

    public PointModel ChargePoint(String userId, Long point) {

        UserModel userInfo = userService.getMyInfo(userId);
        if (userInfo == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "회원정보가 없습니다.");
        }

        PointModel userPoint = pointRepository.findById(userInfo.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));

        return userPoint;
    }


}
