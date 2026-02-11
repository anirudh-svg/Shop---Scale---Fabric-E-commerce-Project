package com.shopscale.order.service;

import com.shopscale.order.domain.Order;
import com.shopscale.order.domain.OrderItem;
import com.shopscale.order.dto.CreateOrderRequest;
import com.shopscale.order.dto.OrderItemRequest;
import com.shopscale.order.exception.OrderNotFoundException;
import com.shopscale.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for order management operations
 * Uses Virtual Threads for high-concurrency processing
 */
@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new order asynchronously using Virtual Threads
     */
    @Async("virtualThreadTaskExecutor")
    public CompletableFuture<Order> createOrderAsync(CreateOrderRequest request) {
        logger.info("Creating order for customer: {}", request.getCustomerId());
        
        try {
            // Calculate total amount
            BigDecimal totalAmount = calculateTotalAmount(request.getItems());
            
            // Create order entity
            Order order = new Order(request.getCustomerId(), totalAmount);
            
            // Add order items
            for (OrderItemRequest itemRequest : request.getItems()) {
                OrderItem orderItem = new OrderItem(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity(),
                    itemRequest.getUnitPrice()
                );
                order.addItem(orderItem);
            }
            
            // Save order to database
            Order savedOrder = orderRepository.save(order);
            logger.info("Order created successfully: {}", savedOrder.getOrderId());
            
            // Publish order placed event asynchronously
            eventPublisher.publishOrderPlacedEventAsync(savedOrder);
            
            return CompletableFuture.completedFuture(savedOrder);
            
        } catch (Exception e) {
            logger.error("Error creating order for customer: {}", request.getCustomerId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Create a new order synchronously
     */
    public Order createOrder(CreateOrderRequest request) {
        logger.info("Creating order synchronously for customer: {}", request.getCustomerId());
        
        // Calculate total amount
        BigDecimal totalAmount = calculateTotalAmount(request.getItems());
        
        // Create order entity
        Order order = new Order(request.getCustomerId(), totalAmount);
        
        // Add order items
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem orderItem = new OrderItem(
                itemRequest.getProductId(),
                itemRequest.getQuantity(),
                itemRequest.getUnitPrice()
            );
            order.addItem(orderItem);
        }
        
        // Save order to database
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully: {}", savedOrder.getOrderId());
        
        // Publish order placed event
        eventPublisher.publishOrderPlacedEvent(savedOrder);
        
        return savedOrder;
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        logger.debug("Retrieving order: {}", orderId);
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    /**
     * Get orders by customer ID
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(String customerId) {
        logger.debug("Retrieving orders for customer: {}", customerId);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Get recent orders for a customer (last 30 days)
     */
    @Transactional(readOnly = true)
    public List<Order> getRecentOrdersByCustomer(String customerId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return orderRepository.findRecentOrdersByCustomer(customerId, thirtyDaysAgo);
    }

    /**
     * Confirm an order
     */
    public Order confirmOrder(String orderId) {
        logger.info("Confirming order: {}", orderId);
        Order order = getOrder(orderId);
        order.confirm();
        return orderRepository.save(order);
    }

    /**
     * Cancel an order
     */
    public Order cancelOrder(String orderId) {
        logger.info("Cancelling order: {}", orderId);
        Order order = getOrder(orderId);
        order.cancel();
        return orderRepository.save(order);
    }

    /**
     * Calculate total amount from order items
     */
    private BigDecimal calculateTotalAmount(List<OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}