package com.shopscale.order.service;

import com.shopscale.order.domain.Order;
import com.shopscale.order.domain.OrderStatus;
import com.shopscale.order.dto.CreateOrderRequest;
import com.shopscale.order.dto.OrderItemRequest;
import com.shopscale.order.exception.OrderNotFoundException;
import com.shopscale.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        // Setup test data
        OrderItemRequest item1 = new OrderItemRequest("prod_001", 2, new BigDecimal("299.99"));
        OrderItemRequest item2 = new OrderItemRequest("prod_002", 1, new BigDecimal("199.99"));
        
        createOrderRequest = new CreateOrderRequest("customer_123", List.of(item1, item2));
        
        savedOrder = new Order("customer_123", new BigDecimal("799.97"));
        savedOrder.setOrderId("order_123");
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        // Given
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId("order_123");
            return order;
        });
        doNothing().when(eventPublisher).publishOrderPlacedEvent(any(Order.class));

        // When
        Order result = orderService.createOrder(createOrderRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo("customer_123");
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("799.97"));
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getItems()).hasSize(2);

        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderPlacedEvent(any(Order.class));
    }

    @Test
    void createOrderAsync_ShouldCreateOrderAsynchronously() throws Exception {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(eventPublisher.publishOrderPlacedEventAsync(any(Order.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<Order> result = orderService.createOrderAsync(createOrderRequest);

        // Then
        Order order = result.get();
        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("customer_123");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("799.97"));

        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderPlacedEventAsync(any(Order.class));
    }

    @Test
    void getOrder_ShouldReturnOrderWhenExists() {
        // Given
        String orderId = "order_123";
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(savedOrder));

        // When
        Order result = orderService.getOrder(orderId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getCustomerId()).isEqualTo("customer_123");

        verify(orderRepository).findByIdWithItems(orderId);
    }

    @Test
    void getOrder_ShouldThrowExceptionWhenNotExists() {
        // Given
        String orderId = "nonexistent_order";
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found: " + orderId);

        verify(orderRepository).findByIdWithItems(orderId);
    }

    @Test
    void getOrdersByCustomer_ShouldReturnCustomerOrders() {
        // Given
        String customerId = "customer_123";
        List<Order> orders = List.of(savedOrder);
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(orders);

        // When
        List<Order> result = orderService.getOrdersByCustomer(customerId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customerId);

        verify(orderRepository).findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Test
    void confirmOrder_ShouldUpdateOrderStatus() {
        // Given
        String orderId = "order_123";
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        Order result = orderService.confirmOrder(orderId);

        // Then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        verify(orderRepository).findByIdWithItems(orderId);
        verify(orderRepository).save(savedOrder);
    }

    @Test
    void cancelOrder_ShouldUpdateOrderStatus() {
        // Given
        String orderId = "order_123";
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        Order result = orderService.cancelOrder(orderId);

        // Then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        verify(orderRepository).findByIdWithItems(orderId);
        verify(orderRepository).save(savedOrder);
    }

    @Test
    void createOrder_ShouldCalculateTotalAmountCorrectly() {
        // Given
        OrderItemRequest item1 = new OrderItemRequest("prod_001", 3, new BigDecimal("100.00"));
        OrderItemRequest item2 = new OrderItemRequest("prod_002", 2, new BigDecimal("50.00"));
        CreateOrderRequest request = new CreateOrderRequest("customer_123", List.of(item1, item2));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventPublisher).publishOrderPlacedEvent(any(Order.class));

        // When
        Order result = orderService.createOrder(request);

        // Then
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("400.00")); // (3*100) + (2*50)
        assertThat(result.getItems()).hasSize(2);
    }
}