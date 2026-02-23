package com.shopscale.cart.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PriceServiceHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public PriceServiceHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("priceService");
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            Health.Builder healthBuilder;

            switch (state) {
                case CLOSED:
                    healthBuilder = Health.up()
                        .withDetail("status", "Circuit breaker is CLOSED - Service is healthy");
                    break;
                case OPEN:
                    healthBuilder = Health.down()
                        .withDetail("status", "Circuit breaker is OPEN - Service is unavailable");
                    break;
                case HALF_OPEN:
                    healthBuilder = Health.unknown()
                        .withDetail("status", "Circuit breaker is HALF_OPEN - Service is recovering");
                    break;
                default:
                    healthBuilder = Health.unknown()
                        .withDetail("status", "Circuit breaker state is unknown");
            }

            return healthBuilder
                .withDetail("circuitBreakerName", "priceService")
                .withDetail("state", state.toString())
                .withDetail("failureRate", String.format("%.2f%%", metrics.getFailureRate()))
                .withDetail("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()))
                .withDetail("bufferedCalls", metrics.getNumberOfBufferedCalls())
                .withDetail("failedCalls", metrics.getNumberOfFailedCalls())
                .withDetail("successfulCalls", metrics.getNumberOfSuccessfulCalls())
                .withDetail("notPermittedCalls", metrics.getNumberOfNotPermittedCalls())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Failed to retrieve circuit breaker status")
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
