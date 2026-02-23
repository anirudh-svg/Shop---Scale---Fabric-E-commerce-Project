package com.shopscale.cart.controller;

import com.shopscale.cart.dto.AddToCartRequest;
import com.shopscale.cart.dto.CartResponse;
import com.shopscale.cart.service.CartService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable String customerId) {
        logger.info("GET /api/cart/{} - Fetching cart", customerId);
        CartResponse cart = cartService.getOrCreateCart(customerId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @PathVariable String customerId,
            @Valid @RequestBody AddToCartRequest request) {
        logger.info("POST /api/cart/{}/items - Adding item: {}", customerId, request.getProductId());
        CartResponse cart = cartService.addItemToCart(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @PutMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable String customerId,
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        logger.info("PUT /api/cart/{}/items/{} - Updating quantity: {}", customerId, productId, quantity);
        CartResponse cart = cartService.updateItemQuantity(customerId, productId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartResponse> removeItemFromCart(
            @PathVariable String customerId,
            @PathVariable String productId) {
        logger.info("DELETE /api/cart/{}/items/{} - Removing item", customerId, productId);
        CartResponse cart = cartService.removeItemFromCart(customerId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> clearCart(@PathVariable String customerId) {
        logger.info("DELETE /api/cart/{} - Clearing cart", customerId);
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}
