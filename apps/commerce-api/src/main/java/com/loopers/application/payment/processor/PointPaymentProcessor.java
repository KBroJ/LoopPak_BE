package com.loopers.application.payment.processor;

import com.loopers.application.payment.PaymentResult;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.PointRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("POINT")
@RequiredArgsConstructor
public class PointPaymentProcessor implements PaymentProcessor {

    private final PointRepository pointRepository;

    @Override
    @Transactional
    public PaymentResult process(PaymentContext context) {
        log.info("포인트 결제 처리 시작 - userId: {}, amount: {}", context.getUserId(), context.getAmount());

        Point userPoint = pointRepository.findByUserIdWithLock(context.getUserId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 포인트 정보를 찾을 수 없습니다."));

        userPoint.use(context.getAmount());  // 잔액 부족시 BAD_REQUEST 발생

        log.info("포인트 결제 완료 - userId: {}, 사용금액: {}, 남은포인트: {}",
                context.getUserId(), context.getAmount(), userPoint.getPoint());

        return PaymentResult.pointSuccess();
    }

}
