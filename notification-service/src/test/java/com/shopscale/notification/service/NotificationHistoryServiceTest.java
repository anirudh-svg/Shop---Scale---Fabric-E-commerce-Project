package com.shopscale.notification.service;

import com.shopscale.notification.domain.NotificationHistory;
import com.shopscale.notification.domain.NotificationStatus;
import com.shopscale.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationHistoryServiceTest {

    private NotificationHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new NotificationHistoryService();
    }

    @Test
    void shouldRecordNotification() {
        // Given
        NotificationHistory notification = createTestNotification("notif-1", "order-1", NotificationStatus.SENT);

        // When
        historyService.recordNotification(notification);

        // Then
        NotificationHistory retrieved = historyService.getNotification("notif-1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getNotificationId()).isEqualTo("notif-1");
        assertThat(retrieved.getOrderId()).isEqualTo("order-1");
    }

    @Test
    void shouldGetNotificationsByOrderId() {
        // Given
        historyService.recordNotification(createTestNotification("notif-1", "order-1", NotificationStatus.SENT));
        historyService.recordNotification(createTestNotification("notif-2", "order-1", NotificationStatus.FAILED));
        historyService.recordNotification(createTestNotification("notif-3", "order-2", NotificationStatus.SENT));

        // When
        List<NotificationHistory> order1Notifications = historyService.getNotificationsByOrderId("order-1");

        // Then
        assertThat(order1Notifications).hasSize(2);
        assertThat(order1Notifications).allMatch(n -> n.getOrderId().equals("order-1"));
    }

    @Test
    void shouldCalculateSuccessCount() {
        // Given
        historyService.recordNotification(createTestNotification("notif-1", "order-1", NotificationStatus.SENT));
        historyService.recordNotification(createTestNotification("notif-2", "order-2", NotificationStatus.SENT));
        historyService.recordNotification(createTestNotification("notif-3", "order-3", NotificationStatus.FAILED));

        // When
        long successCount = historyService.getSuccessCount();

        // Then
        assertThat(successCount).isEqualTo(2);
    }

    @Test
    void shouldCalculateFailureCount() {
        // Given
        historyService.recordNotification(createTestNotification("notif-1", "order-1", NotificationStatus.SENT));
        historyService.recordNotification(createTestNotification("notif-2", "order-2", NotificationStatus.FAILED));
        historyService.recordNotification(createTestNotification("notif-3", "order-3", NotificationStatus.FAILED));

        // When
        long failureCount = historyService.getFailureCount();

        // Then
        assertThat(failureCount).isEqualTo(2);
    }

    @Test
    void shouldGetAllNotifications() {
        // Given
        historyService.recordNotification(createTestNotification("notif-1", "order-1", NotificationStatus.SENT));
        historyService.recordNotification(createTestNotification("notif-2", "order-2", NotificationStatus.SENT));
        historyService.recordNotification(createTestNotification("notif-3", "order-3", NotificationStatus.FAILED));

        // When
        List<NotificationHistory> allNotifications = historyService.getAllNotifications();

        // Then
        assertThat(allNotifications).hasSize(3);
    }

    private NotificationHistory createTestNotification(String notificationId, String orderId, NotificationStatus status) {
        return NotificationHistory.builder()
            .notificationId(notificationId)
            .orderId(orderId)
            .recipientEmail("test@example.com")
            .type(NotificationType.ORDER_CONFIRMATION)
            .status(status)
            .subject("Test Subject")
            .content("Test Content")
            .attemptCount(1)
            .createdAt(LocalDateTime.now())
            .sentAt(status == NotificationStatus.SENT ? LocalDateTime.now() : null)
            .build();
    }
}
