package com.loopers.application.payment.processor;

import com.loopers.application.payment.PaymentResult;

public interface PaymentProcessor {
    PaymentResult process(PaymentContext context);
}
