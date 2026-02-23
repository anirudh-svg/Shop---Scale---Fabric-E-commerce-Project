package com.shopscale.notification.listener;

import com.shopscale.notification.event.OrderItemEvent;
import com.shopscale.notification.event.OrderPlacedEvent;
import com.shopscale.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = {"order-placed"})
@TestPropertySource(locations = "classpath:application-test.yml")
class OrderEventListenerTest {

    @Autowired
    private OrderEventListener orderEventListener;

    @MockBean
    private EmailService emailService;

    @Test
    void shouldProcessOrderPlacedEventSuccessfully() {
        // Given
        OrderPlacedEvent event = createTestOrderEvent();
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        doNothing().when(emailService).sendOrderConfirmation(any(OrderPlacedEvent.class));

        // When
        orderEventListener.handleOrderPlacedEvent(event, 0, 0L, acknowledgment);

        // Then
        verify(emailService, times(1)).sendOrderConfirmation(event);
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void shouldSkipEventWithNoEmail() {
        // Given
        OrderPlacedEvent event = createTestOrderEvent();
        event.setCustomerEmail(null);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        // When
        orderEventListener.handleOrderPlacedEvent(event, 0, 0L, acknowledgment);

        // Then
        verify(emailService, never()).sendOrderConfirmation(any());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void shouldAcknowledgeEvenOnEmailFailure() {
        // Given
        OrderPlacedEvent event = createTestOrderEvent();
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        doThrow(new RuntimeException("Email service error"))
            .when(emailService).sendOrderConfirmation(any(OrderPlacedEvent.class));

        // When
        orderEventListener.handleOrderPlacedEvent(event, 0, 0L, acknowledgment);

        // Then
        verify(emailService, times(1)).sendOrderConfirmation(event);
        verify(acknowledgment, times(1)).acknowledge();
    }

    private OrderPlacedEvent createTestOrderEvent() {
        return OrderPlacedEvent.builder()
            .orderId("order-123")
            .customerId("customer-456")
            .customerEmail("customer@example.com")
            .totalAmount(new BigDecimal("99.99"))
            .timestamp(LocalDateTime.now())
            .items(List.of(
                OrderItemEvent.builder()
                    .productId("product-789")
                    .productName("Test Product")
                    .quantity(2)
                    .unitPrice(new BigDecimal("49.99"))
                    .build()
            ))
            .build();
    }
}
