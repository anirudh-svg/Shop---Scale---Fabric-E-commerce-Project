package com.shopscale.order.service;

import com.shopscale.order.domain.Order;
import com.shopscale.order.domain.OrderItem;
import com.shopscale.order.event.OrderItemEvent;
import com.shopscale.order.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing order-related events to Kafka
 */
@Service
public class OrderEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String ORDER_PLACED_TOPIC = "order-placed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish OrderPlacedEvent synchronously
     */
    public void publishOrderPlacedEvent(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order);
            
            logger.info("Publishing OrderPlacedEvent for order: {}", order.getOrderId());
            
            SendResult<String, Object> result = kafkaTemplate.send(ORDER_PLACED_TOPIC, order.getOrderId(), event).get();
            
            logger.info("OrderPlacedEvent published successfully for order: {} to partition: {} with offset: {}", 
                       order.getOrderId(), 
                       result.getRecordMetadata().partition(), 
                       result.getRecordMetadata().offset());
                       
        } catch (Exception e) {
            logger.error("Failed to publish OrderPlacedEvent for order: {}", order.getOrderId(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }

    /**
     * Publish OrderPlacedEvent asynchronously using Virtual Threads
     */
    @Async("virtualThreadTaskExecutor")
    public CompletableFuture<Void> publishOrderPlacedEventAsync(Order order) {
        try {
            OrderPlacedEvent event = createOrderPlacedEvent(order);
            
            logger.info("Publishing OrderPlacedEvent asynchronously for order: {}", order.getOrderId());
            
            return kafkaTemplate.send(ORDER_PLACED_TOPIC, order.getOrderId(), event)
                    .thenAccept(result -> {
                        logger.info("OrderPlacedEvent published successfully for order: {} to partition: {} with offset: {}", 
                                   order.getOrderId(), 
                                   result.getRecordMetadata().partition(), 
                                   result.getRecordMetadata().offset());
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to publish OrderPlacedEvent for order: {}", order.getOrderId(), throwable);
                        return null;
                    });
                    
        } catch (Exception e) {
            logger.error("Error in async publishing OrderPlacedEvent for order: {}", order.getOrderId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Create OrderPlacedEvent from Order entity
     */
    private OrderPlacedEvent createOrderPlacedEvent(Order order) {
        List<OrderItemEvent> itemEvents = order.getItems().stream()
                .map(this::createOrderItemEvent)
                .toList();

        return new OrderPlacedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                itemEvents,
                order.getTotalAmount()
        );
    }

    /**
     * Create OrderItemEvent from OrderItem entity
     */
    private OrderItemEvent createOrderItemEvent(OrderItem orderItem) {
        return new OrderItemEvent(
                orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice()
        );
    }
}