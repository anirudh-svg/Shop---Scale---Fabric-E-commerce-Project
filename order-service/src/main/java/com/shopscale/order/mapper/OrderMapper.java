package com.shopscale.order.mapper;

import com.shopscale.order.domain.Order;
import com.shopscale.order.domain.OrderItem;
import com.shopscale.order.dto.OrderItemResponse;
import com.shopscale.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Order entities and DTOs
 */
@Component
public class OrderMapper {

    /**
     * Convert Order entity to OrderResponse DTO
     */
    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getStatus(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    /**
     * Convert OrderItem entity to OrderItemResponse DTO
     */
    public OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getTotalPrice()
        );
    }
}