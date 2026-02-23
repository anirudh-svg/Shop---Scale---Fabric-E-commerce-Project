package com.shopscale.cart.repository;

import com.shopscale.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    void deleteByCustomerId(String customerId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.customerId = :customerId")
    Optional<Cart> findByCustomerIdWithItems(String customerId);
}
