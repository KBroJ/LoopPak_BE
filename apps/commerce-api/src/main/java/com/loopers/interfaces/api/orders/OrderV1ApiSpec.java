package com.loopers.interfaces.api.orders;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "주문 API", description = "주문 요청 및 조회 API")
public interface OrderV1ApiSpec {

    @Operation(summary = "주문 요청", description = "상품들을 주문합니다.")
    ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId,
            OrderV1Dto.OrderInfo request
    );

    @Operation(summary = "내 주문 목록 조회", description = "로그인한 사용자의 주문 목록을 조회합니다.")
    ApiResponse<Page<OrderV1Dto.OrderSummary>> getMyOrders(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-USER-ID") Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)") int page,
            @Parameter(description = "페이지당 개수") int size
    );

    @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 내역을 조회합니다.")
    ApiResponse<OrderV1Dto.OrderDetail> getOrderDetail(
            @Parameter(description = "주문 ID") Long orderId
    );

}
