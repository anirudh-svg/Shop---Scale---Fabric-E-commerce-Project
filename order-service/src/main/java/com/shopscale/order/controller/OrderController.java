package com.shopscale.order.controller;

import com.shopscale.order.domain.Order;
import com.shopscale.order.dto.CreateOrderRequest;
import com.shopscale.order.dto.OrderResponse;
import com.shopscale.order.mapper.OrderMapper;
import com.shopscale.order.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for order management operations
 */
@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @Autowired
    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    /**
     * Create a new order
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Received create order request for customer: {}", request.getCustomerId());
        
        Order order = orderService.createOrder(request);
        OrderResponse response = orderMapper.toOrderResponse(order);
        
        logger.info("Order created successfully: {}", order.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a new order asynchronously
     */
    @PostMapping("/async")
    public CompletableFuture<ResponseEntity<OrderResponse>> createOrderAsync(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Received async create order request for customer: {}", request.getCustomerId());
        
        return orderService.createOrderAsync(request)
                .thenApply(order -> {
                    OrderResponse response = orderMapper.toOrderResponse(order);
                    logger.info("Async order created successfully: {}", order.getOrderId());
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to create async order for customer: {}", request.getCustomerId(), throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        logger.debug("Retrieving order: {}", orderId);
        
        Order order = orderService.getOrder(orderId);
        OrderResponse response = orderMapper.toOrderResponse(order);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get orders by customer ID
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable String customerId) {
        logger.debug("Retrieving orders for customer: {}", customerId);
        
        List<Order> orders = orderService.getOrdersByCustomer(customerId);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get recent orders by customer ID
     */
    @GetMapping("/customer/{customerId}/recent")
    public ResponseEntity<List<OrderResponse>> getRecentOrdersByCustomer(@PathVariable String customerId) {
        logger.debug("Retrieving recent orders for customer: {}", customerId);
        
        List<Order> orders = orderService.getRecentOrdersByCustomer(customerId);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Confirm an order
     */
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String orderId) {
        logger.info("Confirming order: {}", orderId);
        
        Order order = orderService.confirmOrder(orderId);
        OrderResponse response = orderMapper.toOrderResponse(order);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        logger.info("Cancelling order: {}", orderId);
        
        Order order = orderService.cancelOrder(orderId);
        OrderResponse response = orderMapper.toOrderResponse(order);
        
        return ResponseEntity.ok(response);
    }
}