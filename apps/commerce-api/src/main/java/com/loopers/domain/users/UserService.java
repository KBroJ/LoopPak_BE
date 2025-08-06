package com.loopers.domain.users;

import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional
    public User saveUser(String userId, String gender, String birthDate, String email) {

        if(userRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.");
        }

        User user = User.of(userId, gender, birthDate, email);
        User savedUser = userRepository.save(user);

        Point point = Point.of(savedUser, 0L);
        pointRepository.save(point);

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User getMyInfo(String userId) {

        return userRepository.findByUserId(userId).orElse(null);

    }

}
