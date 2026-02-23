package com.shopscale.notification.service;

import com.shopscale.notification.event.OrderPlacedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class DeadLetterQueueService {

    private final ConcurrentLinkedQueue<FailedNotification> deadLetterQueue = new ConcurrentLinkedQueue<>();

    public void addToDeadLetterQueue(OrderPlacedEvent event, String errorMessage, int attemptCount) {
        FailedNotification failedNotification = FailedNotification.builder()
            .orderId(event.getOrderId())
            .customerEmail(event.getCustomerEmail())
            .event(event)
            .errorMessage(errorMessage)
            .attemptCount(attemptCount)
            .failedAt(LocalDateTime.now())
            .build();

        deadLetterQueue.offer(failedNotification);
        
        log.error("Added notification to dead letter queue. Order: {}, Email: {}, Error: {}", 
            event.getOrderId(), event.getCustomerEmail(), errorMessage);
    }

    public List<FailedNotification> getDeadLetterQueue() {
        return new ArrayList<>(deadLetterQueue);
    }

    public int getDeadLetterQueueSize() {
        return deadLetterQueue.size();
    }

    public void clearDeadLetterQueue() {
        int size = deadLetterQueue.size();
        deadLetterQueue.clear();
        log.info("Cleared {} items from dead letter queue", size);
    }

    public FailedNotification pollFromDeadLetterQueue() {
        return deadLetterQueue.poll();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedNotification {
        private String orderId;
        private String customerEmail;
        private OrderPlacedEvent event;
        private String errorMessage;
        private int attemptCount;
        private LocalDateTime failedAt;
    }
}
