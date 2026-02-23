package com.shopscale.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Custom filter to handle rate limit exceeded responses
 * Provides detailed error messages when rate limits are hit
 */
@Component
public class RateLimitResponseFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitResponseFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            var statusCode = exchange.getResponse().getStatusCode();
            
            if (statusCode != null && statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                String path = exchange.getRequest().getPath().toString();
                String remoteAddress = exchange.getRequest().getRemoteAddress() != null 
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                    : "unknown";
                
                logger.warn("Rate limit exceeded for IP: {} on path: {}", remoteAddress, path);
                
                // Add custom headers for rate limit information
                exchange.getResponse().getHeaders().add("X-RateLimit-Retry-After", "60");
                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", "100");
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
