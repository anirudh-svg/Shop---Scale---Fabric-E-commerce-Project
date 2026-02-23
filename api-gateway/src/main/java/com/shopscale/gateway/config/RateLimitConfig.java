package com.shopscale.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Rate Limiting Configuration using Redis
 * Implements IP-based rate limiting: 100 requests per minute per IP
 */
@Configuration
public class RateLimitConfig {

    /**
     * Default rate limiter: 100 requests per minute with burst capacity of 200
     */
    @Bean
    @Primary
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(
            100,  // replenishRate: tokens added per second (100/60 = ~1.67 per second)
            200   // burstCapacity: maximum tokens in bucket
        );
    }

    /**
     * Strict rate limiter for sensitive endpoints: 20 requests per minute
     */
    @Bean
    public RedisRateLimiter strictRateLimiter() {
        return new RedisRateLimiter(
            20,   // replenishRate: 20 requests per minute
            40    // burstCapacity: 40 requests burst
        );
    }

    /**
     * IP-based key resolver for rate limiting
     * Extracts client IP address from the request
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ipAddress = Objects.requireNonNull(
                exchange.getRequest().getRemoteAddress()
            ).getAddress().getHostAddress();
            return Mono.just(ipAddress);
        };
    }

    /**
     * User-based key resolver for authenticated requests
     * Uses user ID from JWT token if available, falls back to IP
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from header (set by JWT filter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            
            // Fallback to IP address
            String ipAddress = Objects.requireNonNull(
                exchange.getRequest().getRemoteAddress()
            ).getAddress().getHostAddress();
            return Mono.just(ipAddress);
        };
    }
}
