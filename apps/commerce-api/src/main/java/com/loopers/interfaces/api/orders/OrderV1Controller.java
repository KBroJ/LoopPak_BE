package com.loopers.interfaces.api.orders;

import com.loopers.application.order.OrderApplicationService;
import com.loopers.application.order.OrderDetailResponse;
import com.loopers.application.order.OrderSummaryResponse;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderRequest;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderApplicationService orderAppService;

    @Override
    @PostMapping
    public ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody OrderV1Dto.OrderRequest request) {

        List<OrderItemRequest> itemRequests = request.items().stream()
                .map(item -> new OrderItemRequest(item.productId(), item.quantity()))
                .toList();
        OrderRequest orderRequest = new OrderRequest(itemRequests);

        Order newOrder = orderAppService.placeOrder(userId, orderRequest);

        return ApiResponse.success(OrderV1Dto.OrderResponse.from(newOrder));
    }

    @Override
    @GetMapping
    public ApiResponse<Page<OrderV1Dto.OrderSummary>> getMyOrders(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderSummaryResponse> myOrders = orderAppService.getMyOrders(userId, page, size);
        Page<OrderV1Dto.OrderSummary> response = myOrders.map(OrderV1Dto.OrderSummary::from);

        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderV1Dto.OrderDetail> getOrderDetail(@PathVariable Long orderId) {

        OrderDetailResponse orderDetail = orderAppService.getOrderDetail(orderId);

        return ApiResponse.success(OrderV1Dto.OrderDetail.from(orderDetail));
    }

}
