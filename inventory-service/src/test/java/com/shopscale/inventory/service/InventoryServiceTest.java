package com.shopscale.inventory.service;

import com.shopscale.inventory.domain.InventoryItem;
import com.shopscale.inventory.event.InventoryUpdatedEvent;
import com.shopscale.inventory.exception.InsufficientInventoryException;
import com.shopscale.inventory.exception.InventoryNotFoundException;
import com.shopscale.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private KafkaTemplate<String, InventoryUpdatedEvent> kafkaTemplate;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem testItem;

    @BeforeEach
    void setUp() {
        testItem = new InventoryItem("prod_001", 100);
        testItem.setId(1L);
    }

    @Test
    void createOrUpdateInventory_ShouldCreateNewItem() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        InventoryItem result = inventoryService.createOrUpdateInventory("prod_001", 100);

        // Then
        assertThat(result.getProductId()).isEqualTo("prod_001");
        assertThat(result.getAvailableQuantity()).isEqualTo(100);
        verify(inventoryRepository).save(any(InventoryItem.class));
    }

    @Test
    void createOrUpdateInventory_ShouldUpdateExistingItem() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        InventoryItem result = inventoryService.createOrUpdateInventory("prod_001", 150);

        // Then
        assertThat(result.getAvailableQuantity()).isEqualTo(150);
        verify(inventoryRepository).save(testItem);
    }

    @Test
    void reserveInventory_ShouldReserveSuccessfully() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        inventoryService.reserveInventory("prod_001", 30);

        // Then
        assertThat(testItem.getAvailableQuantity()).isEqualTo(70);
        assertThat(testItem.getReservedQuantity()).isEqualTo(30);
        verify(inventoryRepository).save(testItem);
        verify(kafkaTemplate).send(eq("inventory-updated"), eq("prod_001"), any(InventoryUpdatedEvent.class));
    }

    @Test
    void reserveInventory_ShouldThrowExceptionWhenInsufficientInventory() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));

        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveInventory("prod_001", 150))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient inventory");
        
        verify(inventoryRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void reserveInventory_ShouldThrowExceptionWhenInventoryNotFound() {
        // Given
        when(inventoryRepository.findByProductId("prod_999")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.reserveInventory("prod_999", 10))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("Inventory not found");
    }

    @Test
    void releaseInventory_ShouldReleaseSuccessfully() {
        // Given
        testItem.reserveQuantity(30); // Reserve 30 first
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        inventoryService.releaseInventory("prod_001", 30);

        // Then
        assertThat(testItem.getAvailableQuantity()).isEqualTo(100);
        assertThat(testItem.getReservedQuantity()).isEqualTo(0);
        verify(inventoryRepository).save(testItem);
        verify(kafkaTemplate).send(eq("inventory-updated"), eq("prod_001"), any(InventoryUpdatedEvent.class));
    }

    @Test
    void decreaseInventory_ShouldDecreaseSuccessfully() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        inventoryService.decreaseInventory("prod_001", 25);

        // Then
        assertThat(testItem.getAvailableQuantity()).isEqualTo(75);
        verify(inventoryRepository).save(testItem);
        verify(kafkaTemplate).send(eq("inventory-updated"), eq("prod_001"), any(InventoryUpdatedEvent.class));
    }

    @Test
    void getInventory_ShouldReturnInventoryItem() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));

        // When
        InventoryItem result = inventoryService.getInventory("prod_001");

        // Then
        assertThat(result).isEqualTo(testItem);
    }

    @Test
    void getInventory_ShouldThrowExceptionWhenNotFound() {
        // Given
        when(inventoryRepository.findByProductId("prod_999")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.getInventory("prod_999"))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void isAvailable_ShouldReturnTrueWhenSufficientInventory() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));

        // When
        boolean available = inventoryService.isAvailable("prod_001", 50);

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void isAvailable_ShouldReturnFalseWhenInsufficientInventory() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));

        // When
        boolean available = inventoryService.isAvailable("prod_001", 150);

        // Then
        assertThat(available).isFalse();
    }

    @Test
    void isAvailable_ShouldReturnFalseWhenInventoryNotFound() {
        // Given
        when(inventoryRepository.findByProductId("prod_999")).thenReturn(Optional.empty());

        // When
        boolean available = inventoryService.isAvailable("prod_999", 10);

        // Then
        assertThat(available).isFalse();
    }

    @Test
    void reserveInventory_ShouldPublishCorrectEvent() {
        // Given
        when(inventoryRepository.findByProductId("prod_001")).thenReturn(Optional.of(testItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        // When
        inventoryService.reserveInventory("prod_001", 20);

        // Then
        ArgumentCaptor<InventoryUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryUpdatedEvent.class);
        verify(kafkaTemplate).send(eq("inventory-updated"), eq("prod_001"), eventCaptor.capture());
        
        InventoryUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.getProductId()).isEqualTo("prod_001");
        assertThat(event.getPreviousQuantity()).isEqualTo(100);
        assertThat(event.getNewQuantity()).isEqualTo(80);
        assertThat(event.getTimestamp()).isNotNull();
    }
}
