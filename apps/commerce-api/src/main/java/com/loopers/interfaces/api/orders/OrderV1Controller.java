package com.loopers.interfaces.api.orders;

import com.loopers.application.order.*;
import com.loopers.domain.order.Order;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;
    private final OrderQueryService orderQueryService;

    @Override
    @PostMapping
    public ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody OrderV1Dto.OrderInfo request) {

        List<OrderItemInfo> itemInfos = request.items().stream()
                .map(item -> new OrderItemInfo(item.productId(), item.quantity()))
                .toList();

        // CardInfo 변환 (있을 때만)
        CardInfo cardInfo = null;
        if (request.cardInfo() != null) {
            cardInfo = new CardInfo(
                    request.cardInfo().cardType(),
                    request.cardInfo().cardNo()
            );
        }

        // 주문 요청 객체 생성(포인트 주문)
        OrderInfo orderInfo = new OrderInfo(
            itemInfos,
            request.couponId(),
            request.paymentType() != null ? request.paymentType() : "POINT",    // 기본값 POINT
            null                                                                // 포인트 결제시 PaymentMethod는 null
        );

        Order newOrder = orderFacade.placeOrder(userId, orderInfo);

        return ApiResponse.success(OrderV1Dto.OrderResponse.from(newOrder));
    }

    @Override
    @GetMapping
    public ApiResponse<Page<OrderV1Dto.OrderSummary>> getMyOrders(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderSummaryResponse> myOrders = orderQueryService.getMyOrders(userId, page, size);
        Page<OrderV1Dto.OrderSummary> response = myOrders.map(OrderV1Dto.OrderSummary::from);

        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderV1Dto.OrderDetail> getOrderDetail(@PathVariable Long orderId) {

        OrderDetailResponse orderDetail = orderQueryService.getOrderDetail(orderId);

        return ApiResponse.success(OrderV1Dto.OrderDetail.from(orderDetail));
    }

}
