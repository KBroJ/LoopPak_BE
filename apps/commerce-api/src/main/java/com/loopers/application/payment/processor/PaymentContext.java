package com.loopers.application.payment.processor;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentType;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentContext {

    private final Long userId;
    private final Long orderId;
    private final long amount;
    private final PaymentMethod paymentMethod;  // 카드 결제 시에만 사용

    public static PaymentContext of(
            Long userId, Long orderId, long amount,
            PaymentType paymentType, PaymentMethod paymentMethod
    ) {
        return switch (paymentType) {
            case POINT -> forPoint(userId, amount);
            case CARD -> forCard(userId, orderId, amount, paymentMethod);
            default -> throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 결제 방식입니다: " + paymentType);
        };
    }

    /**
     * 포인트 결제용 Context 생성
     */
    public static PaymentContext forPoint(Long userId, long amount) {
        return new PaymentContext(userId, null, amount, null);
    }

    /**
     * 카드 결제용 Context 생성
     */
    public static PaymentContext forCard(Long userId, Long orderId, long amount, PaymentMethod paymentMethod) {
        return new PaymentContext(userId, orderId, amount, paymentMethod);
    }

}
