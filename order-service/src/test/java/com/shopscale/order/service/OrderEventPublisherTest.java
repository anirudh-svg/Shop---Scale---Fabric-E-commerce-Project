package com.shopscale.order.service;

import com.shopscale.order.domain.Order;
import com.shopscale.order.domain.OrderItem;
import com.shopscale.order.event.OrderPlacedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @Mock
    private RecordMetadata recordMetadata;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order("customer_123", new BigDecimal("299.99"));
        testOrder.setOrderId("order_123");
        
        OrderItem item1 = new OrderItem("prod_001", 2, new BigDecimal("99.99"));
        OrderItem item2 = new OrderItem("prod_002", 1, new BigDecimal("99.99"));
        
        testOrder.addItem(item1);
        testOrder.addItem(item2);
        
        // Setup mock RecordMetadata with lenient stubbing (not all tests use these)
        lenient().when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        lenient().when(recordMetadata.partition()).thenReturn(0);
        lenient().when(recordMetadata.offset()).thenReturn(0L);
    }

    @Test
    void publishOrderPlacedEvent_ShouldPublishEventSuccessfully() throws Exception {
        // Given
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("order-placed"), eq("order_123"), any(OrderPlacedEvent.class)))
                .thenReturn(future);

        // When
        orderEventPublisher.publishOrderPlacedEvent(testOrder);

        // Then
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate).send(eq("order-placed"), eq("order_123"), eventCaptor.capture());
        
        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo("order_123");
        assertThat(capturedEvent.getCustomerId()).isEqualTo("customer_123");
        assertThat(capturedEvent.getTotalAmount()).isEqualTo(new BigDecimal("299.99"));
        assertThat(capturedEvent.getItems()).hasSize(2);
    }

    @Test
    void publishOrderPlacedEvent_ShouldThrowExceptionOnFailure() throws Exception {
        // Given
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(eq("order-placed"), eq("order_123"), any(OrderPlacedEvent.class)))
                .thenReturn(future);

        // When & Then
        assertThatThrownBy(() -> orderEventPublisher.publishOrderPlacedEvent(testOrder))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish order event");
    }

    @Test
    void publishOrderPlacedEventAsync_ShouldPublishEventAsynchronously() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("order-placed"), eq("order_123"), any(OrderPlacedEvent.class)))
                .thenReturn(future);

        // When
        CompletableFuture<Void> result = orderEventPublisher.publishOrderPlacedEventAsync(testOrder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();
        assertThat(result.isCompletedExceptionally()).isFalse();

        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate).send(eq("order-placed"), eq("order_123"), eventCaptor.capture());
        
        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo("order_123");
        assertThat(capturedEvent.getCustomerId()).isEqualTo("customer_123");
    }

    @Test
    void publishOrderPlacedEventAsync_ShouldHandleFailureGracefully() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(eq("order-placed"), eq("order_123"), any(OrderPlacedEvent.class)))
                .thenReturn(future);

        // When
        CompletableFuture<Void> result = orderEventPublisher.publishOrderPlacedEventAsync(testOrder);

        // Then
        assertThat(result).isNotNull();
        // The async method should handle exceptions gracefully and not propagate them
    }
}