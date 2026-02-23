package com.shopscale.inventory.controller;

import com.shopscale.inventory.domain.InventoryItem;
import com.shopscale.inventory.dto.InventoryResponse;
import com.shopscale.inventory.dto.UpdateInventoryRequest;
import com.shopscale.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Get inventory for a specific product
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String productId) {
        log.info("Getting inventory for product: {}", productId);
        InventoryItem item = inventoryService.getInventory(productId);
        return ResponseEntity.ok(toResponse(item));
    }

    /**
     * Check if sufficient inventory is available
     */
    @GetMapping("/{productId}/available")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        log.info("Checking availability for product: {}, quantity: {}", productId, quantity);
        boolean available = inventoryService.isAvailable(productId, quantity);
        return ResponseEntity.ok(available);
    }

    /**
     * Create or update inventory (for admin/testing purposes)
     */
    @PostMapping("/{productId}")
    public ResponseEntity<InventoryResponse> createOrUpdateInventory(
            @PathVariable String productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        log.info("Creating/updating inventory for product: {}, quantity: {}", 
                productId, request.getQuantity());
        InventoryItem item = inventoryService.createOrUpdateInventory(productId, request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(item));
    }

    /**
     * Reserve inventory (for testing purposes)
     */
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Void> reserveInventory(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        log.info("Reserving inventory for product: {}, quantity: {}", productId, quantity);
        inventoryService.reserveInventory(productId, quantity);
        return ResponseEntity.ok().build();
    }

    /**
     * Release inventory (for testing purposes)
     */
    @PostMapping("/{productId}/release")
    public ResponseEntity<Void> releaseInventory(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        log.info("Releasing inventory for product: {}, quantity: {}", productId, quantity);
        inventoryService.releaseInventory(productId, quantity);
        return ResponseEntity.ok().build();
    }

    private InventoryResponse toResponse(InventoryItem item) {
        return new InventoryResponse(
                item.getProductId(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getTotalQuantity(),
                item.getLastUpdated()
        );
    }
}
