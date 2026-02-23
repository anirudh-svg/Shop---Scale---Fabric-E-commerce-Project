package com.shopscale.inventory.listener;

import com.shopscale.inventory.event.OrderItemEvent;
import com.shopscale.inventory.event.OrderPlacedEvent;
import com.shopscale.inventory.exception.InsufficientInventoryException;
import com.shopscale.inventory.exception.InventoryNotFoundException;
import com.shopscale.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for OrderPlacedEvent
 * Consumes events from order-placed topic and updates inventory
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final InventoryService inventoryService;

    public OrderEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Listen to order-placed topic and process inventory updates
     * Uses manual acknowledgment for better error handling
     */
    @KafkaListener(
            topics = "order-placed",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("Received OrderPlacedEvent: orderId={}, partition={}, offset={}", 
                event.getOrderId(), partition, offset);
        
        try {
            processOrderEvent(event);
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed OrderPlacedEvent: orderId={}", event.getOrderId());
            
        } catch (InventoryNotFoundException e) {
            log.error("Inventory not found while processing order: orderId={}, error={}", 
                    event.getOrderId(), e.getMessage());
            // Acknowledge to skip this message (inventory doesn't exist)
            acknowledgment.acknowledge();
            
        } catch (InsufficientInventoryException e) {
            log.error("Insufficient inventory while processing order: orderId={}, error={}", 
                    event.getOrderId(), e.getMessage());
            // Acknowledge to skip this message (not enough inventory)
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing OrderPlacedEvent: orderId={}, error={}", 
                    event.getOrderId(), e.getMessage(), e);
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process order event", e);
        }
    }

    /**
     * Process the order event by reserving inventory for each item
     */
    private void processOrderEvent(OrderPlacedEvent event) {
        log.debug("Processing order items for orderId={}", event.getOrderId());
        
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("Order has no items: orderId={}", event.getOrderId());
            return;
        }
        
        // Reserve inventory for each item in the order
        for (OrderItemEvent item : event.getItems()) {
            try {
                log.debug("Reserving inventory: productId={}, quantity={}", 
                        item.getProductId(), item.getQuantity());
                
                inventoryService.reserveInventory(item.getProductId(), item.getQuantity());
                
                log.info("Reserved inventory: orderId={}, productId={}, quantity={}", 
                        event.getOrderId(), item.getProductId(), item.getQuantity());
                
            } catch (InventoryNotFoundException e) {
                log.error("Product not found in inventory: orderId={}, productId={}", 
                        event.getOrderId(), item.getProductId());
                throw e;
                
            } catch (InsufficientInventoryException e) {
                log.error("Insufficient inventory: orderId={}, productId={}, requested={}", 
                        event.getOrderId(), item.getProductId(), item.getQuantity());
                throw e;
            }
        }
        
        log.info("Completed inventory reservation for order: orderId={}, itemCount={}", 
                event.getOrderId(), event.getItems().size());
    }
}
