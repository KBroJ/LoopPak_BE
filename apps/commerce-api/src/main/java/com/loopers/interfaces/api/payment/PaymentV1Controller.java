package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentStatusInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller implements PaymentV1ApiSpec {

    private final PaymentFacade paymentFacade;

    /**
     * PG에서 호출하는 결제 완료 콜백 엔드포인트
     * 기존 PaymentCallbackController의 로직을 그대로 이동
     */
    @Override
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<String>> handlePaymentCallback(
        @RequestBody PgCallbackRequest request
    ) {

        log.info("PG 콜백 수신 - transactionKey: {}, orderId: {}, status: {}",
                request.transactionKey(), request.orderId(), request.status());

        try {
            // 콜백 처리 로직
            paymentFacade.handlePaymentCallback(request);

            log.info("PG 콜백 처리 완료 - transactionKey: {}", request.transactionKey());

            // PG에게 처리 완료 응답 (PG가 재시도하지 않도록)
            return ResponseEntity.ok(ApiResponse.success("결제 콜백 처리 완료"));

        } catch (Exception e) {
            log.error("PG 콜백 처리 실패 - transactionKey: {}, error: {}",
                    request.transactionKey(), e.getMessage(), e);

            // PG에게 실패 응답 (PG가 재시도할 수 있도록)
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success("콜백 처리 실패"));
        }
    }

    /**
     * 콜백 엔드포인트 헬스체크 (PG에서 연결 확인용)
     */
    @Override
    @GetMapping("/callback/health")
    public ResponseEntity<String> callbackHealth() {
        return ResponseEntity.ok("Callback endpoint is healthy");
    }

    /**
     * 결제 상태 확인 - PG에서 최신 상태 조회하여 동기화
     */
    @Override
    @GetMapping("/{transactionKey}/status")
    public ApiResponse<PaymentV1Dto.PaymentStatusResponse> checkPaymentStatus(
            @PathVariable("transactionKey") String transactionKey,
            @RequestHeader("X-USER-ID") String userId) {

        PaymentStatusInfo response = paymentFacade.checkPaymentStatus(transactionKey, userId);
        return ApiResponse.success(
                PaymentV1Dto.PaymentStatusResponse.from(response)
        );
    }

    /**
     * 결제 상태 수동 동기화 - 관리자용 또는 배치용
     */
    @Override
    @PostMapping("/sync-status")
    public ApiResponse<PaymentV1Dto.PaymentStatusResponse> syncPaymentStatus(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PaymentV1Dto.PaymentSyncRequest request) {

        PaymentStatusInfo response = paymentFacade.syncPaymentStatus(request.transactionKey(), userId);
        return ApiResponse.success(
                PaymentV1Dto.PaymentStatusResponse.from(response)
        );
    }

}
