package com.loopers.application.product;

/**
 * 재고 변경 사유를 정의하는 enum
 *
 * 장점:
 * - 타입 안전성: 잘못된 reason 값 사용 방지
 * - 일관성: 모든 재고 변경 사유를 한 곳에서 관리
 * - 확장성: 새로운 사유 추가 시 컴파일러가 누락된 처리 알림
 * - 분석: 재고 변경 원인별 통계 및 분석 가능
 */
public enum StockChangeReason {

    // 재고 감소 사유
    ORDER("주문", "고객 주문으로 인한 재고 차감"),
    DAMAGE("손상", "상품 손상으로 인한 재고 차감"),
    LOSS("분실", "상품 분실로 인한 재고 차감"),
    EXPIRED("유통기한", "유통기한 만료로 인한 재고 차감"),
    SAMPLE("샘플", "샘플 제공으로 인한 재고 차감"),

    //  재고 증가 사유
    CANCEL("주문취소", "주문 취소로 인한 재고 복구"),
    RETURN("반품", "고객 반품으로 인한 재고 복구"),
    RESTOCK("신규입고", "신규 입고로 인한 재고 증가"),
    ADJUSTMENT("재고조정", "관리자 재고 조정"),
    FOUND("재발견", "분실 상품 재발견으로 인한 재고 증가");

    private final String displayName;
    private final String description;

    StockChangeReason(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    // 재고 감소 사유들
    public boolean isDecreaseReason() {
        return switch (this) {
            case ORDER, DAMAGE, LOSS, EXPIRED, SAMPLE -> true;
            default -> false;
        };
    }

    // 재고 증가 사유들
    public boolean isIncreaseReason() {
        return switch (this) {
            case CANCEL, RETURN, RESTOCK, ADJUSTMENT, FOUND -> true;
            default -> false;
        };
    }

    // 주문 관련 사유인지 확인
    public boolean isOrderRelated() {
        return this == ORDER || this == CANCEL;
    }

    // 외부 요인 사유인지 확인 (손상, 분실 등)
    public boolean isExternalFactor() {
        return switch (this) {
            case DAMAGE, LOSS, EXPIRED -> true;
            default -> false;
        };
    }

}
