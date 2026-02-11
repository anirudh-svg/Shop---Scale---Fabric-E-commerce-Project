package com.shopscale.order.repository;

import com.shopscale.order.domain.Order;
import com.shopscale.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Find orders by customer ID
     */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    /**
     * Find orders by customer ID with pagination
     */
    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders by status with pagination
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders created between dates
     */
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find orders by customer and status
     */
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

    /**
     * Find orders with items using JOIN FETCH to avoid N+1 problem
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") String orderId);

    /**
     * Find recent orders for a customer
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByCustomer(@Param("customerId") String customerId, @Param("since") LocalDateTime since);
}