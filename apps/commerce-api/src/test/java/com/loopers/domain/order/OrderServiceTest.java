package com.loopers.domain.order;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.users.UserModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("주문 생성 시, 재고와 포인트가 차감되고 주문이 저장된다.")
    void placeOrder_deductsStockAndPoints_andSavesOrder() {
        // given
        UserModel user = new UserModel("testuser", "MALE", "2000-01-01", "test@test.com");
        PointModel userPoint = new PointModel(user, 50000L);
        Product product1 = Product.of(1L, "상품1", "", 10000, 10, 10, ProductStatus.ACTIVE);
        int initialStock = product1.getStock();
        long initialPoints = userPoint.getPoint();

        List<Product> products = List.of(product1);
        Map<Long, Integer> quantityMap = Map.of(product1.getId(), 2);

        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Order resultOrder = orderService.placeOrder(1L, userPoint, products, quantityMap);

        // then
        assertThat(product1.getStock()).isEqualTo(initialStock - 2);
        assertThat(userPoint.getPoint()).isEqualTo(initialPoints - (10000 * 2));
        assertThat(resultOrder.getUserId()).isEqualTo(1L);
        assertThat(resultOrder.calculateTotalPrice()).isEqualTo(20000L);
        verify(orderRepository).save(any(Order.class));
    }

}
