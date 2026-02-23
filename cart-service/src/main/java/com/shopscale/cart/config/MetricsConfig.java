package com.shopscale.cart.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;

    public MetricsConfig(CircuitBreakerRegistry circuitBreakerRegistry, MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerCircuitBreakerMetrics() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerMetrics);
    }

    private void registerMetrics(CircuitBreaker circuitBreaker) {
        String name = circuitBreaker.getName();

        // Register failure rate gauge
        Gauge.builder("circuit.breaker.failure.rate", circuitBreaker,
                cb -> cb.getMetrics().getFailureRate())
            .tag("name", name)
            .description("Circuit breaker failure rate percentage")
            .register(meterRegistry);

        // Register slow call rate gauge
        Gauge.builder("circuit.breaker.slow.call.rate", circuitBreaker,
                cb -> cb.getMetrics().getSlowCallRate())
            .tag("name", name)
            .description("Circuit breaker slow call rate percentage")
            .register(meterRegistry);

        // Register buffered calls gauge
        Gauge.builder("circuit.breaker.buffered.calls", circuitBreaker,
                cb -> cb.getMetrics().getNumberOfBufferedCalls())
            .tag("name", name)
            .description("Number of buffered calls in circuit breaker")
            .register(meterRegistry);

        // Register failed calls gauge
        Gauge.builder("circuit.breaker.failed.calls", circuitBreaker,
                cb -> cb.getMetrics().getNumberOfFailedCalls())
            .tag("name", name)
            .description("Number of failed calls in circuit breaker")
            .register(meterRegistry);

        // Register successful calls gauge
        Gauge.builder("circuit.breaker.successful.calls", circuitBreaker,
                cb -> cb.getMetrics().getNumberOfSuccessfulCalls())
            .tag("name", name)
            .description("Number of successful calls in circuit breaker")
            .register(meterRegistry);

        // Register not permitted calls gauge
        Gauge.builder("circuit.breaker.not.permitted.calls", circuitBreaker,
                cb -> cb.getMetrics().getNumberOfNotPermittedCalls())
            .tag("name", name)
            .description("Number of not permitted calls when circuit breaker is open")
            .register(meterRegistry);

        // Register state gauge (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
        Gauge.builder("circuit.breaker.state", circuitBreaker, cb -> {
                switch (cb.getState()) {
                    case CLOSED: return 0;
                    case OPEN: return 1;
                    case HALF_OPEN: return 2;
                    default: return -1;
                }
            })
            .tag("name", name)
            .description("Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
            .register(meterRegistry);
    }
}
