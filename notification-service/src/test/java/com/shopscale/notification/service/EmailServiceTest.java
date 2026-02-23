package com.shopscale.notification.service;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.shopscale.notification.domain.NotificationStatus;
import com.shopscale.notification.event.OrderItemEvent;
import com.shopscale.notification.event.OrderPlacedEvent;
import com.shopscale.notification.exception.EmailSendingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration"
})
@TestPropertySource(locations = "classpath:application-test.yml")
class EmailServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
        .withConfiguration(GreenMailConfiguration.aConfig().withUser("test@shopscale.com", "test"))
        .withPerMethodLifecycle(false);

    @Autowired
    private NotificationHistoryService historyService;

    @Autowired
    private DeadLetterQueueService deadLetterQueueService;

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        greenMail.reset();
    }

    @Test
    void shouldSendOrderConfirmationSuccessfully() {
        // Given
        OrderPlacedEvent event = createTestOrderEvent("order-success-123");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(event);

        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        
        // Verify notification history
        var notifications = historyService.getNotificationsByOrderId(event.getOrderId());
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.stream().anyMatch(n -> 
            n.getStatus() == NotificationStatus.SENT && 
            n.getRecipientEmail().equals(event.getCustomerEmail())
        )).isTrue();
    }

    @Test
    void shouldRetryOnFailureAndEventuallySucceed() throws Exception {
        // Given
        OrderPlacedEvent event = createTestOrderEvent("order-retry-123");
        
        // Fail first two attempts, succeed on third
        doThrow(new MailSendException("Connection timeout"))
            .doThrow(new MailSendException("Connection timeout"))
            .doNothing()
            .when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(event);

        // Then - Spring Retry should have retried 3 times total
        // Wait a bit for async retry operations
        Thread.sleep(500);
        
        verify(mailSender, atLeast(1)).send(any(SimpleMailMessage.class));
        
        var notifications = historyService.getNotificationsByOrderId(event.getOrderId());
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(notifications.size() - 1).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void shouldAddToDeadLetterQueueAfterMaxRetries() throws Exception {
        // Given
        OrderPlacedEvent event = createTestOrderEvent("order-failed-123");
        
        // Fail all attempts
        doThrow(new MailSendException("Connection timeout"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        // When
        try {
            emailService.sendOrderConfirmation(event);
        } catch (Exception e) {
            // Expected to fail after retries
        }

        // Then - wait for retry attempts to complete
        Thread.sleep(1000);
        
        // Verify added to dead letter queue
        assertThat(deadLetterQueueService.getDeadLetterQueueSize()).isGreaterThan(0);
        
        // Verify notification history shows failure
        var notifications = historyService.getNotificationsByOrderId(event.getOrderId());
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.stream().anyMatch(n -> n.getStatus() == NotificationStatus.FAILED)).isTrue();
    }

    @Test
    void shouldBuildCorrectEmailContent() {
        // Given
        OrderPlacedEvent event = createTestOrderEvent("order-content-123");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendOrderConfirmation(event);

        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        
        var notifications = historyService.getNotificationsByOrderId(event.getOrderId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getContent()).contains("Thank you for your order");
        assertThat(notifications.get(0).getContent()).contains(event.getOrderId());
        assertThat(notifications.get(0).getContent()).contains("$99.99");
    }

    private OrderPlacedEvent createTestOrderEvent(String orderId) {
        return OrderPlacedEvent.builder()
            .orderId(orderId)
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
