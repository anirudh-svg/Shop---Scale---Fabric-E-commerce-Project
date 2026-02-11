package com.shopscale.order.domain;

/**
 * Order status enumeration
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    COMPLETED
}