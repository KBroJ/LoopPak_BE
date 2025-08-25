package com.loopers.interfaces.api.payment;

public record PgCallbackRequest(
    String transactionKey,
    String orderId,
    String status,          // "SUCCESS" | "FAILED" | "CANCELLED"
    Long amount,
    String message,         // 성공/실패 메시지
    String cardType,        // "SAMSUNG"
    String processedAt      // PG 처리 완료 시간 (ISO 8601)
) {
    // PG 응답이 성공인지 확인
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    // 실패인지 확인
    public boolean isFailed() {
        return "FAILED".equals(status) || "CANCELLED".equals(status);
    }
}
