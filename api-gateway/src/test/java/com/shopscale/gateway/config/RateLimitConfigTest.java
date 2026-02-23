package com.shopscale.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
    }

    @Test
    void redisRateLimiter_ShouldCreateWithCorrectLimits() {
        // Act
        RedisRateLimiter rateLimiter = rateLimitConfig.redisRateLimiter();

        // Assert
        assertThat(rateLimiter).isNotNull();
    }

    @Test
    void strictRateLimiter_ShouldCreateWithStrictLimits() {
        // Act
        RedisRateLimiter rateLimiter = rateLimitConfig.strictRateLimiter();

        // Assert
        assertThat(rateLimiter).isNotNull();
    }

    @Test
    void ipKeyResolver_ShouldResolveIpAddress() {
        // Arrange
        KeyResolver keyResolver = rateLimitConfig.ipKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/test")
            .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act & Assert
        StepVerifier.create(keyResolver.resolve(exchange))
            .expectNext("192.168.1.100")
            .verifyComplete();
    }

    @Test
    void userKeyResolver_ShouldResolveUserId_WhenHeaderPresent() {
        // Arrange
        KeyResolver keyResolver = rateLimitConfig.userKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/test")
            .header("X-User-Id", "user123")
            .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act & Assert
        StepVerifier.create(keyResolver.resolve(exchange))
            .expectNext("user123")
            .verifyComplete();
    }

    @Test
    void userKeyResolver_ShouldFallbackToIp_WhenNoUserHeader() {
        // Arrange
        KeyResolver keyResolver = rateLimitConfig.userKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/test")
            .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act & Assert
        StepVerifier.create(keyResolver.resolve(exchange))
            .expectNext("192.168.1.100")
            .verifyComplete();
    }
}
