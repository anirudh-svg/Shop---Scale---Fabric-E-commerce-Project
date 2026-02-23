package com.shopscale.cart.service;

import com.shopscale.cart.domain.Cart;
import com.shopscale.cart.domain.CartItem;
import com.shopscale.cart.dto.AddToCartRequest;
import com.shopscale.cart.dto.CartResponse;
import com.shopscale.cart.dto.PriceResponse;
import com.shopscale.cart.exception.CartNotFoundException;
import com.shopscale.cart.mapper.CartMapper;
import com.shopscale.cart.repository.CartRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final PriceService priceService;
    private final CartMapper cartMapper;

    public CartService(CartRepository cartRepository, PriceService priceService, CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.priceService = priceService;
        this.cartMapper = cartMapper;
    }

    @Transactional
    public CartResponse getOrCreateCart(String customerId) {
        logger.info("Getting or creating cart for customer: {}", customerId);
        
        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
            .orElseGet(() -> {
                Cart newCart = new Cart(customerId);
                return cartRepository.save(newCart);
            });

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCart(String customerId, AddToCartRequest request) {
        logger.info("Adding item to cart for customer: {}, productId: {}", customerId, request.getProductId());

        // Get or create cart
        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
            .orElseGet(() -> new Cart(customerId));

        // Get product price from Price Service (with circuit breaker)
        // The circuit breaker will handle failures and return fallback price if needed
        PriceResponse priceResponse;
        try {
            priceResponse = priceService.getProductPrice(request.getProductId()).join();
        } catch (Exception e) {
            logger.error("Failed to fetch price for product: {}", request.getProductId(), e);
            throw new RuntimeException("Unable to fetch product price", e);
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
            .filter(item -> item.getProductId().equals(request.getProductId()))
            .findFirst();

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.updateQuantity(item.getQuantity() + request.getQuantity());
            logger.info("Updated existing cart item quantity: {}", item.getQuantity());
        } else {
            // Add new item
            CartItem newItem = new CartItem(
                request.getProductId(),
                priceResponse.getProductName(),
                request.getQuantity(),
                priceResponse.getPrice()
            );
            cart.addItem(newItem);
            logger.info("Added new item to cart: {}", request.getProductId());
        }

        cart.recalculateTotal();
        Cart savedCart = cartRepository.save(cart);

        return cartMapper.toResponse(savedCart);
    }

    @Transactional
    public CartResponse updateItemQuantity(String customerId, String productId, Integer quantity) {
        logger.info("Updating item quantity for customer: {}, productId: {}, quantity: {}", 
            customerId, productId, quantity);

        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found for customer: " + customerId));

        CartItem item = cart.getItems().stream()
            .filter(i -> i.getProductId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new CartNotFoundException("Item not found in cart: " + productId));

        if (quantity <= 0) {
            cart.removeItem(item);
            logger.info("Removed item from cart: {}", productId);
        } else {
            item.updateQuantity(quantity);
            logger.info("Updated item quantity: {}", quantity);
        }

        cart.recalculateTotal();
        Cart savedCart = cartRepository.save(cart);

        return cartMapper.toResponse(savedCart);
    }

    @Transactional
    public CartResponse removeItemFromCart(String customerId, String productId) {
        logger.info("Removing item from cart for customer: {}, productId: {}", customerId, productId);

        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found for customer: " + customerId));

        CartItem item = cart.getItems().stream()
            .filter(i -> i.getProductId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new CartNotFoundException("Item not found in cart: " + productId));

        cart.removeItem(item);
        cart.recalculateTotal();
        Cart savedCart = cartRepository.save(cart);

        return cartMapper.toResponse(savedCart);
    }

    @Transactional
    public void clearCart(String customerId) {
        logger.info("Clearing cart for customer: {}", customerId);

        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found for customer: " + customerId));

        cart.clearItems();
        cartRepository.save(cart);
    }
}
