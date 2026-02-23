package com.shopscale.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private String orderId;
    private String customerId;
    private String customerEmail;
    private List<OrderItemEvent> items;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;
}
