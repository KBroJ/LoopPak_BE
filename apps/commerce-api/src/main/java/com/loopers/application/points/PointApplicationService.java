package com.loopers.application.points;

import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.domain.users.User;
import com.loopers.domain.users.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PointApplicationService {

    private final UserRepository userRepository;
    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public PointInfo getPointInfo(String userId) {

        User user = userRepository.findByUserId(userId) // 실제 메소드명은 다를 수 있습니다.
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "회원정보가 없습니다."));

        Point point = pointRepository.findByUserId(user.getId())
                .orElse(Point.of(user, 0L)); // 포인트 정보가 없으면 0점으로 생성해서 반환

        return PointInfo.from(point);
    }

    @Transactional
    public PointInfo chargePoint(String userId, Long amountToCharge) {
        // 1. 유스케이스의 시작: 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "회원정보가 없습니다."));

        // 2. 비관적 락을 사용하여 포인트 정보를 조회합니다. (동시성 제어)
        //    (PointRepository에 findByUserIdWithLock 메소드가 추가되었다고 가정)
        Point point = pointRepository.findByUserIdWithLock(user.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));

        // 3. 도메인 객체에 메시지를 보내 비즈니스 로직을 위임합니다.
        // 4. @Transactional에 의해 변경 감지(Dirty checking)로 자동 저장되므로 save 호출은 불필요.
        point.charge(amountToCharge);

        // 5. 결과를 DTO로 변환하여 반환
        return PointInfo.from(point);
    }

    @Transactional(readOnly = true)
    public Point getPointByUserId(Long userId) {
        return pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 포인트 정보를 찾을 수 없습니다."));
    }

}
