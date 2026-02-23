package com.shopscale.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {
    private String notificationId;
    private String orderId;
    private String recipientEmail;
    private NotificationType type;
    private NotificationStatus status;
    private String subject;
    private String content;
    private Integer attemptCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime lastAttemptAt;
}
