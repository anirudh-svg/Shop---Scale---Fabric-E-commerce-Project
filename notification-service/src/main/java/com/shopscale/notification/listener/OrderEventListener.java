package com.shopscale.notification.listener;

import com.shopscale.notification.event.OrderPlacedEvent;
import com.shopscale.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailService emailService;

    @KafkaListener(
        topics = "order-placed",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderPlacedEvent(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        log.info("Received OrderPlacedEvent for order: {} from partition: {}, offset: {}", 
            event.getOrderId(), partition, offset);

        try {
            // Validate event
            if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
                log.warn("OrderPlacedEvent for order {} has no customer email. Skipping notification.", 
                    event.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // Send order confirmation email with retry logic
            emailService.sendOrderConfirmation(event);

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed OrderPlacedEvent for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing OrderPlacedEvent for order: {}. Error: {}", 
                event.getOrderId(), e.getMessage(), e);
            
            // In production, implement dead letter queue logic here
            // For now, we acknowledge to prevent infinite retries at Kafka level
            // The retry logic is handled by @Retryable in EmailService
            acknowledgment.acknowledge();
        }
    }
}
