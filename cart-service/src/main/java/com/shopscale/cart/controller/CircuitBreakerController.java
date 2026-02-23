package com.shopscale.cart.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/circuit-breaker")
public class CircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus(@PathVariable String name) {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            Map<String, Object> status = new HashMap<>();
            status.put("name", name);
            status.put("state", circuitBreaker.getState().toString());
            status.put("metrics", Map.of(
                "failureRate", String.format("%.2f%%", metrics.getFailureRate()),
                "slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()),
                "bufferedCalls", metrics.getNumberOfBufferedCalls(),
                "failedCalls", metrics.getNumberOfFailedCalls(),
                "successfulCalls", metrics.getNumberOfSuccessfulCalls(),
                "slowCalls", metrics.getNumberOfSlowCalls(),
                "notPermittedCalls", metrics.getNumberOfNotPermittedCalls()
            ));

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCircuitBreakers() {
        Map<String, Object> allCircuitBreakers = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            allCircuitBreakers.put(cb.getName(), Map.of(
                "state", cb.getState().toString(),
                "failureRate", String.format("%.2f%%", metrics.getFailureRate()),
                "bufferedCalls", metrics.getNumberOfBufferedCalls(),
                "failedCalls", metrics.getNumberOfFailedCalls()
            ));
        });

        return ResponseEntity.ok(allCircuitBreakers);
    }
}
