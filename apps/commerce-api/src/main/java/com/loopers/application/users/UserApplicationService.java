package com.loopers.application.users;

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
public class UserApplicationService {

    private final UserRepository userRepository;
    private final PointRepository pointRepository;

    /**
     * 신규 사용자를 생성하고, 기본 포인트를 지급
     */
    @Transactional
    public UserInfo saveUser(String userId, String gender, String birthDate, String email) {
        // 1. 비즈니스 규칙 검증 (ID 중복 체크)
        if(userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.");
        }

        // 2. 도메인 객체 생성 위임
        User user = User.of(userId, gender, birthDate, email);
        User savedUser = userRepository.save(user);

        // 3. 다른 도메인(Point)과의 상호작용
        Point point = Point.of(savedUser, 0L);
        pointRepository.save(point);

        // 4. 결과를 DTO로 변환하여 반환
        return UserInfo.from(savedUser);
    }

    /**
     * 내 정보를 조회
     */
    @Transactional(readOnly = true)
    public UserInfo getMyInfo(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        return UserInfo.from(user);
    }

}
