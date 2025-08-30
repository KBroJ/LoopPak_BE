package com.loopers.application.userlog.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 유저 행동 이벤트를 처리하는 핸들러
 *
 * 목적:
 * 1. 모든 유저 행동을 통합적으로 로깅
 * 2. 메인 비즈니스 로직과 로깅 로직의 분리
 * 3. 추후 데이터 플랫폼이나 분석 시스템으로 전송 가능
 */
@Slf4j
@Component
public class UserActionEventHandler {

    /**
     * 유저 행동 이벤트를 받아서 로깅 처리
     */
    @EventListener
    public void handleUserAction(UserActionEvent event) {
        log.info("handleUserAction|[유저행동 로깅 이벤트] userId: {}, action: {}, targetType: {}, targetId: {}, details: {}, timestamp: {}",
            event.userId(),
            event.action(),
            event.targetType(),
            event.targetId(),
            event.details(),
            event.timestamp()
        );

    }

}
