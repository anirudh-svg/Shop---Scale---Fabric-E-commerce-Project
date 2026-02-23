package com.shopscale.inventory.repository;

import com.shopscale.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    /**
     * Find inventory item by product ID with pessimistic write lock
     * to prevent concurrent modification issues
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findByProductId(String productId);

    /**
     * Check if inventory exists for a product
     */
    boolean existsByProductId(String productId);
}
