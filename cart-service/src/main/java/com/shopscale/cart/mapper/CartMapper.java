package com.shopscale.cart.mapper;

import com.shopscale.cart.domain.Cart;
import com.shopscale.cart.domain.CartItem;
import com.shopscale.cart.dto.CartItemResponse;
import com.shopscale.cart.dto.CartResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        return new CartResponse(
            cart.getId(),
            cart.getCustomerId(),
            cart.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList()),
            cart.getTotalAmount(),
            cart.getCreatedAt(),
            cart.getUpdatedAt()
        );
    }

    public CartItemResponse toItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }

        return new CartItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getSubtotal()
        );
    }
}
