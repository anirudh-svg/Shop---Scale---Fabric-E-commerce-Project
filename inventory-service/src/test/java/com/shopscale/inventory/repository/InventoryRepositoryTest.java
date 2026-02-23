package com.shopscale.inventory.repository;

import com.shopscale.inventory.domain.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void findByProductId_ShouldReturnInventoryItem() {
        // Given
        InventoryItem item = new InventoryItem("prod_001", 100);
        inventoryRepository.save(item);

        // When
        Optional<InventoryItem> found = inventoryRepository.findByProductId("prod_001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo("prod_001");
        assertThat(found.get().getAvailableQuantity()).isEqualTo(100);
        assertThat(found.get().getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void findByProductId_ShouldReturnEmptyWhenNotFound() {
        // When
        Optional<InventoryItem> found = inventoryRepository.findByProductId("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByProductId_ShouldReturnTrueWhenExists() {
        // Given
        InventoryItem item = new InventoryItem("prod_002", 50);
        inventoryRepository.save(item);

        // When
        boolean exists = inventoryRepository.existsByProductId("prod_002");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByProductId_ShouldReturnFalseWhenNotExists() {
        // When
        boolean exists = inventoryRepository.existsByProductId("nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void save_ShouldPersistInventoryItem() {
        // Given
        InventoryItem item = new InventoryItem("prod_003", 75);

        // When
        InventoryItem saved = inventoryRepository.save(item);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo("prod_003");
        assertThat(saved.getAvailableQuantity()).isEqualTo(75);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getLastUpdated()).isNotNull();
    }

    @Test
    void save_ShouldUpdateExistingInventoryItem() {
        // Given
        InventoryItem item = new InventoryItem("prod_004", 100);
        InventoryItem saved = inventoryRepository.save(item);

        // When
        saved.setAvailableQuantity(150);
        InventoryItem updated = inventoryRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getAvailableQuantity()).isEqualTo(150);
        assertThat(updated.getLastUpdated()).isNotNull();
    }

    @Test
    void inventoryItem_ShouldEnforceUniqueProductId() {
        // Given
        InventoryItem item1 = new InventoryItem("prod_005", 100);
        inventoryRepository.save(item1);

        // When & Then
        InventoryItem item2 = new InventoryItem("prod_005", 200);
        try {
            inventoryRepository.saveAndFlush(item2);
            assertThat(false).as("Should have thrown exception for duplicate productId").isTrue();
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e).isNotNull();
        }
    }
}
