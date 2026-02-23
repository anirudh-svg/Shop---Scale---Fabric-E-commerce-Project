package com.shopscale.notification.service;

import com.shopscale.notification.domain.NotificationHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class NotificationHistoryService {

    // In-memory storage for notification history
    // In production, this would be persisted to a database
    private final Map<String, NotificationHistory> notificationHistory = new ConcurrentHashMap<>();
    private final List<NotificationHistory> allNotifications = new ArrayList<>();

    public void recordNotification(NotificationHistory notification) {
        log.info("Recording notification: {} for order: {} with status: {}", 
            notification.getNotificationId(), 
            notification.getOrderId(), 
            notification.getStatus());
        
        notificationHistory.put(notification.getNotificationId(), notification);
        allNotifications.add(notification);
    }

    public NotificationHistory getNotification(String notificationId) {
        return notificationHistory.get(notificationId);
    }

    public List<NotificationHistory> getNotificationsByOrderId(String orderId) {
        return allNotifications.stream()
            .filter(n -> n.getOrderId().equals(orderId))
            .toList();
    }

    public List<NotificationHistory> getAllNotifications() {
        return new ArrayList<>(allNotifications);
    }

    public long getSuccessCount() {
        return allNotifications.stream()
            .filter(n -> n.getStatus() == com.shopscale.notification.domain.NotificationStatus.SENT)
            .count();
    }

    public long getFailureCount() {
        return allNotifications.stream()
            .filter(n -> n.getStatus() == com.shopscale.notification.domain.NotificationStatus.FAILED)
            .count();
    }
}
