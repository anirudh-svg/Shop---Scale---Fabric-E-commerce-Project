package com.shopscale.inventory.service;

import com.shopscale.inventory.domain.InventoryItem;
import com.shopscale.inventory.event.InventoryUpdatedEvent;
import com.shopscale.inventory.exception.InsufficientInventoryException;
import com.shopscale.inventory.exception.InventoryNotFoundException;
import com.shopscale.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String INVENTORY_UPDATED_TOPIC = "inventory-updated";

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, InventoryUpdatedEvent> kafkaTemplate;

    public InventoryService(InventoryRepository inventoryRepository,
                           KafkaTemplate<String, InventoryUpdatedEvent> kafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Create or update inventory for a product
     */
    @Transactional
    public InventoryItem createOrUpdateInventory(String productId, Integer quantity) {
        log.info("Creating/updating inventory for product: {}, quantity: {}", productId, quantity);
        
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElse(new InventoryItem(productId, quantity));
        
        if (item.getId() == null) {
            // New item
            item = inventoryRepository.save(item);
            log.info("Created new inventory item for product: {}", productId);
        } else {
            // Update existing
            Integer previousQuantity = item.getAvailableQuantity();
            item.setAvailableQuantity(quantity);
            item = inventoryRepository.save(item);
            log.info("Updated inventory for product: {} from {} to {}", 
                    productId, previousQuantity, quantity);
        }
        
        return item;
    }

    /**
     * Reserve inventory for an order
     */
    @Transactional
    public void reserveInventory(String productId, Integer quantity) {
        log.info("Reserving inventory for product: {}, quantity: {}", productId, quantity);
        
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
        
        if (item.getAvailableQuantity() < quantity) {
            throw new InsufficientInventoryException(
                    String.format("Insufficient inventory for product %s. Available: %d, Requested: %d",
                            productId, item.getAvailableQuantity(), quantity));
        }
        
        Integer previousQuantity = item.getAvailableQuantity();
        item.reserveQuantity(quantity);
        inventoryRepository.save(item);
        
        log.info("Reserved {} units for product: {}. Available: {} -> {}", 
                quantity, productId, previousQuantity, item.getAvailableQuantity());
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(productId, previousQuantity, item.getAvailableQuantity());
    }

    /**
     * Release reserved inventory (e.g., when order is cancelled)
     */
    @Transactional
    public void releaseInventory(String productId, Integer quantity) {
        log.info("Releasing inventory for product: {}, quantity: {}", productId, quantity);
        
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
        
        Integer previousQuantity = item.getAvailableQuantity();
        item.releaseQuantity(quantity);
        inventoryRepository.save(item);
        
        log.info("Released {} units for product: {}. Available: {} -> {}", 
                quantity, productId, previousQuantity, item.getAvailableQuantity());
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(productId, previousQuantity, item.getAvailableQuantity());
    }

    /**
     * Decrease inventory (e.g., when order is fulfilled)
     */
    @Transactional
    public void decreaseInventory(String productId, Integer quantity) {
        log.info("Decreasing inventory for product: {}, quantity: {}", productId, quantity);
        
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
        
        Integer previousQuantity = item.getAvailableQuantity();
        item.decreaseQuantity(quantity);
        inventoryRepository.save(item);
        
        log.info("Decreased {} units for product: {}. Available: {} -> {}", 
                quantity, productId, previousQuantity, item.getAvailableQuantity());
        
        // Publish inventory updated event
        publishInventoryUpdatedEvent(productId, previousQuantity, item.getAvailableQuantity());
    }

    /**
     * Get inventory for a product
     */
    @Transactional(readOnly = true)
    public InventoryItem getInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));
    }

    /**
     * Check if sufficient inventory is available
     */
    @Transactional(readOnly = true)
    public boolean isAvailable(String productId, Integer quantity) {
        return inventoryRepository.findByProductId(productId)
                .map(item -> item.getAvailableQuantity() >= quantity)
                .orElse(false);
    }

    /**
     * Publish inventory updated event to Kafka
     */
    private void publishInventoryUpdatedEvent(String productId, Integer previousQuantity, Integer newQuantity) {
        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                productId,
                previousQuantity,
                newQuantity,
                LocalDateTime.now()
        );
        
        kafkaTemplate.send(INVENTORY_UPDATED_TOPIC, productId, event);
        log.info("Published InventoryUpdatedEvent for product: {}", productId);
    }
}
