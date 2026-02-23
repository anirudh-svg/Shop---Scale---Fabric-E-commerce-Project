package com.shopscale.gateway.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    // Configuration beans moved to specific config classes:
    // - RateLimitConfig: Rate limiting and key resolvers
    // - SecurityConfig: Security and JWT configuration
}
