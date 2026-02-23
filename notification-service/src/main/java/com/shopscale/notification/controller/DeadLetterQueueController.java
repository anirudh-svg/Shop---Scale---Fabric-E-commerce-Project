package com.shopscale.notification.controller;

import com.shopscale.notification.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dlq")
@RequiredArgsConstructor
public class DeadLetterQueueController {

    private final DeadLetterQueueService deadLetterQueueService;

    @GetMapping
    public ResponseEntity<List<DeadLetterQueueService.FailedNotification>> getDeadLetterQueue() {
        return ResponseEntity.ok(deadLetterQueueService.getDeadLetterQueue());
    }

    @GetMapping("/size")
    public ResponseEntity<Map<String, Integer>> getDeadLetterQueueSize() {
        Map<String, Integer> response = new HashMap<>();
        response.put("size", deadLetterQueueService.getDeadLetterQueueSize());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearDeadLetterQueue() {
        deadLetterQueueService.clearDeadLetterQueue();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Dead letter queue cleared successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/retry")
    public ResponseEntity<Map<String, String>> retryFailedNotification() {
        DeadLetterQueueService.FailedNotification failed = deadLetterQueueService.pollFromDeadLetterQueue();
        if (failed == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "No failed notifications to retry");
            return ResponseEntity.ok(response);
        }

        // In a real system, this would re-publish the event to Kafka
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification retry initiated for order: " + failed.getOrderId());
        return ResponseEntity.ok(response);
    }
}
