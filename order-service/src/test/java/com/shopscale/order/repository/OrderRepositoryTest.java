package com.shopscale.order.repository;

import com.shopscale.order.domain.Order;
import com.shopscale.order.domain.OrderItem;
import com.shopscale.order.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Create test order
        testOrder = new Order("customer_123", new BigDecimal("299.99"));
        
        // Add order items
        OrderItem item1 = new OrderItem("prod_001", 2, new BigDecimal("99.99"));
        OrderItem item2 = new OrderItem("prod_002", 1, new BigDecimal("99.99"));
        
        testOrder.addItem(item1);
        testOrder.addItem(item2);
        
        entityManager.persistAndFlush(testOrder);
    }

    @Test
    void findByCustomerIdOrderByCreatedAtDesc_ShouldReturnOrdersForCustomer() {
        // When
        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc("customer_123");

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getCustomerId()).isEqualTo("customer_123");
        assertThat(orders.get(0).getTotalAmount()).isEqualTo(new BigDecimal("299.99"));
    }

    @Test
    void findByStatus_ShouldReturnOrdersWithSpecificStatus() {
        // When
        List<Order> orders = orderRepository.findByStatus(OrderStatus.PENDING);

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByIdWithItems_ShouldReturnOrderWithItems() {
        // When
        Optional<Order> orderOpt = orderRepository.findByIdWithItems(testOrder.getOrderId());

        // Then
        assertThat(orderOpt).isPresent();
        Order order = orderOpt.get();
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).getProductId()).isIn("prod_001", "prod_002");
    }

    @Test
    void findByCustomerIdAndStatus_ShouldReturnMatchingOrders() {
        // When
        List<Order> orders = orderRepository.findByCustomerIdAndStatus("customer_123", OrderStatus.PENDING);

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getCustomerId()).isEqualTo("customer_123");
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void countByStatus_ShouldReturnCorrectCount() {
        // When
        long count = orderRepository.countByStatus(OrderStatus.PENDING);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void findRecentOrdersByCustomer_ShouldReturnRecentOrders() {
        // Given
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        // When
        List<Order> orders = orderRepository.findRecentOrdersByCustomer("customer_123", oneDayAgo);

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getCreatedAt()).isAfter(oneDayAgo);
    }

    @Test
    void save_ShouldPersistOrderWithItems() {
        // Given
        Order newOrder = new Order("customer_456", new BigDecimal("199.99"));
        OrderItem item = new OrderItem("prod_003", 1, new BigDecimal("199.99"));
        newOrder.addItem(item);

        // When
        Order savedOrder = orderRepository.save(newOrder);

        // Then
        assertThat(savedOrder.getOrderId()).isNotNull();
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getItems().get(0).getOrder()).isEqualTo(savedOrder);
    }
}