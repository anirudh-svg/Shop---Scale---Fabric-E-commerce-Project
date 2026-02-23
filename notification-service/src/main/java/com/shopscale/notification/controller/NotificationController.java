package com.shopscale.notification.controller;

import com.shopscale.notification.domain.NotificationHistory;
import com.shopscale.notification.service.NotificationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationHistoryService historyService;

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationHistory> getNotification(@PathVariable String notificationId) {
        NotificationHistory notification = historyService.getNotification(notificationId);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<NotificationHistory>> getNotificationsByOrder(@PathVariable String orderId) {
        List<NotificationHistory> notifications = historyService.getNotificationsByOrderId(orderId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping
    public ResponseEntity<List<NotificationHistory>> getAllNotifications() {
        List<NotificationHistory> notifications = historyService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotifications", historyService.getAllNotifications().size());
        stats.put("successfulNotifications", historyService.getSuccessCount());
        stats.put("failedNotifications", historyService.getFailureCount());
        stats.put("successRate", calculateSuccessRate());
        return ResponseEntity.ok(stats);
    }

    private double calculateSuccessRate() {
        long total = historyService.getAllNotifications().size();
        if (total == 0) {
            return 0.0;
        }
        long success = historyService.getSuccessCount();
        return (double) success / total * 100.0;
    }
}
