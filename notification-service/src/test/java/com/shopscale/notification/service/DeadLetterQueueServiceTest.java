package com.shopscale.notification.service;

import com.shopscale.notification.event.OrderItemEvent;
import com.shopscale.notification.event.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeadLetterQueueServiceTest {

    private DeadLetterQueueService deadLetterQueueService;

    @BeforeEach
    void setUp() {
        deadLetterQueueService = new DeadLetterQueueService();
    }

    @Test
    void shouldAddToDeadLetterQueue() {
        // Given
        OrderPlacedEvent event = createTestOrderEvent();

        // When
        deadLetterQueueService.addToDeadLetterQueue(event, "Email sending failed", 3);

        // Then
        assertThat(deadLetterQueueService.getDeadLetterQueueSize()).isEqualTo(1);
    }

    @Test
    void shouldGetDeadLetterQueue() {
        // Given
        OrderPlacedEvent event1 = createTestOrderEvent();
        OrderPlacedEvent event2 = createTestOrderEvent();
        event2.setOrderId("order-456");

        deadLetterQueueService.addToDeadLetterQueue(event1, "Error 1", 3);
        deadLetterQueueService.addToDeadLetterQueue(event2, "Error 2", 3);

        // When
        List<DeadLetterQueueService.FailedNotification> queue = deadLetterQueueService.getDeadLetterQueue();

        // Then
        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).getOrderId()).isEqualTo("order-123");
        assertThat(queue.get(1).getOrderId()).isEqualTo("order-456");
    }

    @Test
    void shouldPollFromDeadLetterQueue() {
        // Given
        OrderPlacedEvent event = createTestOrderEvent();
        deadLetterQueueService.addToDeadLetterQueue(event, "Email sending failed", 3);

        // When
        DeadLetterQueueService.FailedNotification polled = deadLetterQueueService.pollFromDeadLetterQueue();

        // Then
        assertThat(polled).isNotNull();
        assertThat(polled.getOrderId()).isEqualTo("order-123");
        assertThat(deadLetterQueueService.getDeadLetterQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldClearDeadLetterQueue() {
        // Given
        OrderPlacedEvent event1 = createTestOrderEvent();
        OrderPlacedEvent event2 = createTestOrderEvent();
        event2.setOrderId("order-456");

        deadLetterQueueService.addToDeadLetterQueue(event1, "Error 1", 3);
        deadLetterQueueService.addToDeadLetterQueue(event2, "Error 2", 3);

        // When
        deadLetterQueueService.clearDeadLetterQueue();

        // Then
        assertThat(deadLetterQueueService.getDeadLetterQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldReturnNullWhenPollingEmptyQueue() {
        // When
        DeadLetterQueueService.FailedNotification polled = deadLetterQueueService.pollFromDeadLetterQueue();

        // Then
        assertThat(polled).isNull();
    }

    private OrderPlacedEvent createTestOrderEvent() {
        return OrderPlacedEvent.builder()
            .orderId("order-123")
            .customerId("customer-456")
            .customerEmail("customer@example.com")
            .totalAmount(new BigDecimal("99.99"))
            .timestamp(LocalDateTime.now())
            .items(List.of(
                OrderItemEvent.builder()
                    .productId("product-789")
                    .productName("Test Product")
                    .quantity(2)
                    .unitPrice(new BigDecimal("49.99"))
                    .build()
            ))
            .build();
    }
}
