package com.shopscale.cart.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerEventListener.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerEventListener(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostConstruct
    public void registerEventListeners() {
        circuitBreakerRegistry.getEventPublisher()
            .onEntryAdded(this::onCircuitBreakerAdded);
    }

    private void onCircuitBreakerAdded(EntryAddedEvent<CircuitBreaker> event) {
        CircuitBreaker circuitBreaker = event.getAddedEntry();
        String circuitBreakerName = circuitBreaker.getName();

        logger.info("Circuit breaker '{}' registered", circuitBreakerName);

        // Register state transition listener
        circuitBreaker.getEventPublisher()
            .onStateTransition(this::onStateTransition);

        // Register success listener
        circuitBreaker.getEventPublisher()
            .onSuccess(this::onSuccess);

        // Register error listener
        circuitBreaker.getEventPublisher()
            .onError(this::onError);
    }

    private void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        logger.warn("Circuit breaker '{}' state transition: {} -> {}",
            event.getCircuitBreakerName(),
            event.getStateTransition().getFromState(),
            event.getStateTransition().getToState());

        // Log metrics
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(event.getCircuitBreakerName());
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        logger.info("Circuit breaker '{}' metrics - Failure rate: {}%, Slow call rate: {}%, Buffered calls: {}, Failed calls: {}",
            event.getCircuitBreakerName(),
            metrics.getFailureRate(),
            metrics.getSlowCallRate(),
            metrics.getNumberOfBufferedCalls(),
            metrics.getNumberOfFailedCalls());
    }

    private void onSuccess(CircuitBreakerOnSuccessEvent event) {
        logger.debug("Circuit breaker '{}' recorded successful call - Duration: {}ms",
            event.getCircuitBreakerName(),
            event.getElapsedDuration().toMillis());
    }

    private void onError(CircuitBreakerOnErrorEvent event) {
        logger.error("Circuit breaker '{}' recorded failed call - Duration: {}ms, Error: {}",
            event.getCircuitBreakerName(),
            event.getElapsedDuration().toMillis(),
            event.getThrowable().getMessage());
    }
}
