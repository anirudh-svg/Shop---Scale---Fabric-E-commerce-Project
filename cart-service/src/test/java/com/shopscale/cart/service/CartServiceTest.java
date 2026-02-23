package com.shopscale.cart.service;

import com.shopscale.cart.domain.Cart;
import com.shopscale.cart.domain.CartItem;
import com.shopscale.cart.dto.AddToCartRequest;
import com.shopscale.cart.dto.CartResponse;
import com.shopscale.cart.dto.PriceResponse;
import com.shopscale.cart.exception.CartNotFoundException;
import com.shopscale.cart.mapper.CartMapper;
import com.shopscale.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PriceService priceService;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    private String customerId;
    private String productId;
    private Cart cart;
    private PriceResponse priceResponse;

    @BeforeEach
    void setUp() {
        customerId = "customer-123";
        productId = "product-456";
        cart = new Cart(customerId);
        priceResponse = new PriceResponse(productId, "Test Product", BigDecimal.valueOf(29.99), "USD");
    }

    @Test
    void testGetOrCreateCart_ExistingCart() {
        // Given
        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.getOrCreateCart(customerId);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).findByCustomerIdWithItems(customerId);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateCart_NewCart() {
        // Given
        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.getOrCreateCart(customerId);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).findByCustomerIdWithItems(customerId);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void testAddItemToCart_NewItem_WithCircuitBreaker() {
        // Given
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(2);

        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(priceService.getProductPrice(productId))
            .thenReturn(CompletableFuture.completedFuture(priceResponse));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.addItemToCart(customerId, request);

        // Then
        assertThat(response).isNotNull();
        verify(priceService).getProductPrice(productId);
        verify(cartRepository).save(any(Cart.class));
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void testAddItemToCart_ExistingItem_UpdatesQuantity() {
        // Given
        CartItem existingItem = new CartItem(productId, "Test Product", 1, BigDecimal.valueOf(29.99));
        cart.addItem(existingItem);

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(2);

        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(priceService.getProductPrice(productId))
            .thenReturn(CompletableFuture.completedFuture(priceResponse));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.addItemToCart(customerId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3); // 1 + 2
    }

    @Test
    void testAddItemToCart_WithFallbackPrice() {
        // Given - Circuit breaker returns fallback price
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(1);

        PriceResponse fallbackPrice = new PriceResponse(
            productId, 
            "Product " + productId, 
            BigDecimal.valueOf(99.99), 
            "USD"
        );

        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(priceService.getProductPrice(productId))
            .thenReturn(CompletableFuture.completedFuture(fallbackPrice));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.addItemToCart(customerId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
    }

    @Test
    void testUpdateItemQuantity_Success() {
        // Given
        CartItem item = new CartItem(productId, "Test Product", 2, BigDecimal.valueOf(29.99));
        cart.addItem(item);

        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.updateItemQuantity(customerId, productId, 5);

        // Then
        assertThat(response).isNotNull();
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(cartRepository).save(cart);
    }

    @Test
    void testUpdateItemQuantity_RemovesItemWhenQuantityZero() {
        // Given
        CartItem item = new CartItem(productId, "Test Product", 2, BigDecimal.valueOf(29.99));
        cart.addItem(item);

        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.updateItemQuantity(customerId, productId, 0);

        // Then
        assertThat(response).isNotNull();
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void testUpdateItemQuantity_CartNotFound() {
        // Given
        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> cartService.updateItemQuantity(customerId, productId, 5))
            .isInstanceOf(CartNotFoundException.class)
            .hasMessageContaining("Cart not found for customer");
    }

    @Test
    void testRemoveItemFromCart_Success() {
        // Given
        CartItem item = new CartItem(productId, "Test Product", 2, BigDecimal.valueOf(29.99));
        cart.addItem(item);

        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(new CartResponse());

        // When
        CartResponse response = cartService.removeItemFromCart(customerId, productId);

        // Then
        assertThat(response).isNotNull();
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void testClearCart_Success() {
        // Given
        cart.addItem(new CartItem(productId, "Test Product", 2, BigDecimal.valueOf(29.99)));
        cart.addItem(new CartItem("product-789", "Another Product", 1, BigDecimal.valueOf(19.99)));

        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // When
        cartService.clearCart(customerId);

        // Then
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }
}
