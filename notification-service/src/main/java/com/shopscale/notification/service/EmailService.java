package com.shopscale.notification.service;

import com.shopscale.notification.domain.NotificationHistory;
import com.shopscale.notification.domain.NotificationStatus;
import com.shopscale.notification.domain.NotificationType;
import com.shopscale.notification.event.OrderPlacedEvent;
import com.shopscale.notification.exception.EmailSendingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationHistoryService historyService;
    private final DeadLetterQueueService deadLetterQueueService;

    @Retryable(
        retryFor = {EmailSendingException.class, MailException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 10000)
    )
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        log.info("Attempting to send order confirmation email for order: {}", event.getOrderId());
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getCustomerEmail());
            message.setSubject("Order Confirmation - Order #" + event.getOrderId());
            message.setText(buildOrderConfirmationContent(event));
            message.setFrom("noreply@shopscale.com");

            mailSender.send(message);
            
            log.info("Successfully sent order confirmation email for order: {}", event.getOrderId());
            
            // Record successful notification
            historyService.recordNotification(
                NotificationHistory.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .orderId(event.getOrderId())
                    .recipientEmail(event.getCustomerEmail())
                    .type(NotificationType.ORDER_CONFIRMATION)
                    .status(NotificationStatus.SENT)
                    .subject(message.getSubject())
                    .content(message.getText())
                    .attemptCount(1)
                    .createdAt(LocalDateTime.now())
                    .sentAt(LocalDateTime.now())
                    .build()
            );
            
        } catch (MailException e) {
            log.error("Failed to send order confirmation email for order: {}", event.getOrderId(), e);
            throw new EmailSendingException("Failed to send email for order: " + event.getOrderId(), e);
        }
    }

    @Recover
    public void recoverFromEmailFailure(EmailSendingException e, OrderPlacedEvent event) {
        log.error("All retry attempts exhausted for order: {}. Moving to dead letter queue.", 
            event.getOrderId(), e);
        
        // Record failed notification
        historyService.recordNotification(
            NotificationHistory.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(event.getOrderId())
                .recipientEmail(event.getCustomerEmail())
                .type(NotificationType.ORDER_CONFIRMATION)
                .status(NotificationStatus.FAILED)
                .subject("Order Confirmation - Order #" + event.getOrderId())
                .attemptCount(3)
                .errorMessage(e.getMessage())
                .createdAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .build()
        );
        
        // Add to dead letter queue
        deadLetterQueueService.addToDeadLetterQueue(event, e.getMessage(), 3);
    }

    @Recover
    public void recoverFromMailException(MailException e, OrderPlacedEvent event) {
        log.error("All retry attempts exhausted for order: {} due to MailException. Moving to dead letter queue.", 
            event.getOrderId(), e);
        
        historyService.recordNotification(
            NotificationHistory.builder()
                .notificationId(UUID.randomUUID().toString())
                .orderId(event.getOrderId())
                .recipientEmail(event.getCustomerEmail())
                .type(NotificationType.ORDER_CONFIRMATION)
                .status(NotificationStatus.FAILED)
                .subject("Order Confirmation - Order #" + event.getOrderId())
                .attemptCount(3)
                .errorMessage(e.getMessage())
                .createdAt(LocalDateTime.now())
                .lastAttemptAt(LocalDateTime.now())
                .build()
        );
        
        // Add to dead letter queue
        deadLetterQueueService.addToDeadLetterQueue(event, e.getMessage(), 3);
    }

    private String buildOrderConfirmationContent(OrderPlacedEvent event) {
        StringBuilder content = new StringBuilder();
        content.append("Dear Customer,\n\n");
        content.append("Thank you for your order!\n\n");
        content.append("Order Details:\n");
        content.append("Order ID: ").append(event.getOrderId()).append("\n");
        content.append("Order Date: ").append(event.getTimestamp()).append("\n");
        content.append("Total Amount: $").append(event.getTotalAmount()).append("\n\n");
        content.append("Items:\n");
        
        event.getItems().forEach(item -> {
            content.append("- ").append(item.getProductName())
                .append(" (Qty: ").append(item.getQuantity())
                .append(", Price: $").append(item.getUnitPrice()).append(")\n");
        });
        
        content.append("\nWe will send you another email when your order ships.\n\n");
        content.append("Thank you for shopping with ShopScale!\n\n");
        content.append("Best regards,\n");
        content.append("The ShopScale Team");
        
        return content.toString();
    }
}
