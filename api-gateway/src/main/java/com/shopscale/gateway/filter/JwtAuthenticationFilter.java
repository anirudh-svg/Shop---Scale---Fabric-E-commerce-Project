package com.shopscale.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT Authentication Filter
 * Validates JWT tokens and adds authentication information to downstream requests
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMap(authentication -> {
                if (authentication != null && authentication.isAuthenticated()) {
                    logger.debug("Authenticated request for user: {}", authentication.getName());
                    
                    // Add user information to request headers for downstream services
                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Id", authentication.getName())
                                      .header("X-User-Roles", authentication.getAuthorities().toString()))
                        .build();
                    
                    return chain.filter(mutatedExchange);
                } else {
                    logger.warn("Unauthenticated request to protected endpoint: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            })
            .switchIfEmpty(Mono.defer(() -> {
                logger.warn("No security context found for request to: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }));
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator") || 
               path.startsWith("/fallback");
    }

    @Override
    public int getOrder() {
        return -100; // Run after logging filter but before other filters
    }
}
