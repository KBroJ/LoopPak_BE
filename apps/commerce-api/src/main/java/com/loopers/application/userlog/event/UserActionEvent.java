package com.loopers.application.userlog.event;

import java.time.LocalDateTime;

public record UserActionEvent(
    Long userId,            // 행동을 수행한 사용자 ID
    String action,          // 행동 타입 (PRODUCT_VIEW, SEARCH, LIKE_ADDED 등)
    String targetType,      // 대상 타입 (PRODUCT, ORDER 등)
    Long targetId,          // 대상의 구체적인 ID
    String details,         // 추가 상세 정보
    LocalDateTime timestamp
) {
    
    public static UserActionEvent productView(Long userId, Long productId) {
        return new UserActionEvent(
            userId,
            "PRODUCT_VIEW",
            "PRODUCT", 
            productId,
            "상품 상세 조회",
            LocalDateTime.now()
        );
    }
    
    public static UserActionEvent productSearch(Long userId, String query, String sort, int totalResults) {
        String details = String.format("검색어: %s, 정렬: %s, 결과수: %d", query, sort, totalResults);
        return new UserActionEvent(
            userId,
            "PRODUCT_SEARCH",
            "SEARCH",
            null,
            details,
            LocalDateTime.now()
        );
    }
    
    public static UserActionEvent likeAction(Long userId, Long targetId, String likeType, boolean isAdded) {
        String action = isAdded ? "LIKE_ADDED" : "LIKE_REMOVED";
        String details = String.format("%s에 좋아요 %s", likeType, isAdded ? "추가" : "제거");
        return new UserActionEvent(
            userId,
            action,
            likeType,
            targetId,
            details,
            LocalDateTime.now()
        );
    }
    
    public static UserActionEvent orderAction(Long userId, Long orderId, String orderStatus, Long totalAmount) {
        String details = String.format("주문 상태: %s, 총 금액: %d원", orderStatus, totalAmount);
        return new UserActionEvent(
            userId,
            "ORDER_CREATED",
            "ORDER",
            orderId,
            details,
            LocalDateTime.now()
        );
    }
}