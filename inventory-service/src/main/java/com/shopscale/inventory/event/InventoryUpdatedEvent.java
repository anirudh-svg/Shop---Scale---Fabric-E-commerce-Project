package com.shopscale.inventory.event;

import java.time.LocalDateTime;

public class InventoryUpdatedEvent {

    private String productId;
    private Integer previousQuantity;
    private Integer newQuantity;
    private LocalDateTime timestamp;

    public InventoryUpdatedEvent() {
    }

    public InventoryUpdatedEvent(String productId, Integer previousQuantity, Integer newQuantity, LocalDateTime timestamp) {
        this.productId = productId;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getPreviousQuantity() {
        return previousQuantity;
    }

    public void setPreviousQuantity(Integer previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    public Integer getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "InventoryUpdatedEvent{" +
                "productId='" + productId + '\'' +
                ", previousQuantity=" + previousQuantity +
                ", newQuantity=" + newQuantity +
                ", timestamp=" + timestamp +
                '}';
    }
}
