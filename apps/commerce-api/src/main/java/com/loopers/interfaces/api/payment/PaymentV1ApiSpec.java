package com.loopers.interfaces.api.payment;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Payment V1 API", description = "결제 콜백 수신 및 상태 확인 API")
public interface PaymentV1ApiSpec {

    @Operation(
            summary = "PG 결제 콜백 처리",
            description = "PG에서 호출하는 결제 완료 콜백을 처리합니다."
    )
    ResponseEntity<ApiResponse<String>> handlePaymentCallback(
            @Schema(description = "PG 콜백 데이터")
            PgCallbackRequest request
    );

    @Operation(
            summary = "콜백 엔드포인트 헬스체크",
            description = "PG에서 연결 확인용으로 사용하는 헬스체크 엔드포인트입니다."
    )
    ResponseEntity<String> callbackHealth();

    @Operation(
            summary = "결제 상태 확인",
            description = "PG에서 최신 결제 상태를 조회하고 우리 시스템과 동기화합니다."
    )
    ApiResponse<PaymentV1Dto.PaymentStatusResponse> checkPaymentStatus(
            @Schema(description = "결제 트랜잭션 키")
            @PathVariable("transactionKey") String transactionKey,
            @Schema(description = "사용자 ID")
            @RequestHeader("X-USER-ID") String userId
    );

    @Operation(
            summary = "결제 상태 수동 동기화",
            description = "특정 결제건의 상태를 PG와 강제 동기화합니다."
    )
    ApiResponse<PaymentV1Dto.PaymentStatusResponse> syncPaymentStatus(
            @Schema(description = "사용자 ID")
            @RequestHeader("X-USER-ID") String userId,
            @Schema(description = "동기화 요청 데이터")
            PaymentV1Dto.PaymentSyncRequest request
    );

}
