package com.shopscale.gateway.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Utility class for JWT token operations
 */
@Component
public class JwtUtil {

    /**
     * Extract user ID from JWT token
     */
    public String extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("sub");
        }
        return null;
    }

    /**
     * Extract user email from JWT token
     */
    public String extractEmail(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("email");
        }
        return null;
    }

    /**
     * Extract user roles from JWT token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                return (List<String>) realmAccess.get("roles");
            }
        }
        return List.of();
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(Authentication authentication, String role) {
        return extractRoles(authentication).contains(role);
    }
}
