package com.shopscale.inventory.listener;

import com.shopscale.inventory.config.TestKafkaConfig;
import com.shopscale.inventory.domain.InventoryItem;
import com.shopscale.inventory.event.OrderItemEvent;
import com.shopscale.inventory.event.OrderPlacedEvent;
import com.shopscale.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-placed", "inventory-updated"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderEventListenerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        
        // Create initial inventory
        InventoryItem item1 = new InventoryItem("prod_001", 100);
        InventoryItem item2 = new InventoryItem("prod_002", 50);
        inventoryRepository.save(item1);
        inventoryRepository.save(item2);
    }

    @Test
    void handleOrderPlaced_ShouldReserveInventory() {
        // Given
        OrderItemEvent item1 = new OrderItemEvent("prod_001", 10, new BigDecimal("99.99"));
        OrderItemEvent item2 = new OrderItemEvent("prod_002", 5, new BigDecimal("49.99"));
        
        OrderPlacedEvent event = new OrderPlacedEvent(
                "order_123",
                "customer_456",
                Arrays.asList(item1, item2),
                new BigDecimal("1249.85")
        );
        event.setTimestamp(LocalDateTime.now());

        // When
        kafkaTemplate.send("order-placed", "order_123", event);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            InventoryItem inventory1 = inventoryRepository.findByProductId("prod_001").orElseThrow();
            assertThat(inventory1.getAvailableQuantity()).isEqualTo(90);
            assertThat(inventory1.getReservedQuantity()).isEqualTo(10);

            InventoryItem inventory2 = inventoryRepository.findByProductId("prod_002").orElseThrow();
            assertThat(inventory2.getAvailableQuantity()).isEqualTo(45);
            assertThat(inventory2.getReservedQuantity()).isEqualTo(5);
        });
    }

    @Test
    void handleOrderPlaced_ShouldHandleMultipleOrders() {
        // Given
        OrderPlacedEvent event1 = createOrderEvent("order_001", "prod_001", 20);
        OrderPlacedEvent event2 = createOrderEvent("order_002", "prod_001", 15);

        // When
        kafkaTemplate.send("order-placed", "order_001", event1);
        kafkaTemplate.send("order-placed", "order_002", event2);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            InventoryItem inventory = inventoryRepository.findByProductId("prod_001").orElseThrow();
            assertThat(inventory.getAvailableQuantity()).isEqualTo(65); // 100 - 20 - 15
            assertThat(inventory.getReservedQuantity()).isEqualTo(35); // 20 + 15
        });
    }

    @Test
    void handleOrderPlaced_ShouldHandleInsufficientInventory() {
        // Given - Try to reserve more than available
        OrderPlacedEvent event = createOrderEvent("order_003", "prod_002", 100);

        // When
        kafkaTemplate.send("order-placed", "order_003", event);

        // Then - Inventory should remain unchanged
        await().pollDelay(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            InventoryItem inventory = inventoryRepository.findByProductId("prod_002").orElseThrow();
            assertThat(inventory.getAvailableQuantity()).isEqualTo(50); // Unchanged
            assertThat(inventory.getReservedQuantity()).isEqualTo(0); // Unchanged
        });
    }

    @Test
    void handleOrderPlaced_ShouldHandleNonExistentProduct() {
        // Given
        OrderPlacedEvent event = createOrderEvent("order_004", "prod_999", 10);

        // When
        kafkaTemplate.send("order-placed", "order_004", event);

        // Then - Should not throw exception, just log and skip
        await().pollDelay(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify other inventory items are not affected
            InventoryItem inventory1 = inventoryRepository.findByProductId("prod_001").orElseThrow();
            assertThat(inventory1.getAvailableQuantity()).isEqualTo(100);
        });
    }

    @Test
    void handleOrderPlaced_ShouldProcessEventsInOrder() {
        // Given - Multiple events for the same product
        OrderPlacedEvent event1 = createOrderEvent("order_005", "prod_001", 10);
        OrderPlacedEvent event2 = createOrderEvent("order_006", "prod_001", 20);
        OrderPlacedEvent event3 = createOrderEvent("order_007", "prod_001", 15);

        // When
        kafkaTemplate.send("order-placed", "order_005", event1);
        kafkaTemplate.send("order-placed", "order_006", event2);
        kafkaTemplate.send("order-placed", "order_007", event3);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            InventoryItem inventory = inventoryRepository.findByProductId("prod_001").orElseThrow();
            assertThat(inventory.getAvailableQuantity()).isEqualTo(55); // 100 - 10 - 20 - 15
            assertThat(inventory.getReservedQuantity()).isEqualTo(45); // 10 + 20 + 15
        });
    }

    private OrderPlacedEvent createOrderEvent(String orderId, String productId, Integer quantity) {
        OrderItemEvent item = new OrderItemEvent(productId, quantity, new BigDecimal("99.99"));
        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId,
                "customer_123",
                Arrays.asList(item),
                new BigDecimal("99.99").multiply(new BigDecimal(quantity))
        );
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}
