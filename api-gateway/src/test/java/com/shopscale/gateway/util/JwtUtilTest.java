package com.shopscale.gateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void extractUserId_ShouldReturnSubject() {
        // Arrange
        Jwt jwt = createTestJwt("user123", "test@example.com", List.of("USER"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        // Act
        String userId = jwtUtil.extractUserId(authentication);

        // Assert
        assertThat(userId).isEqualTo("user123");
    }

    @Test
    void extractEmail_ShouldReturnEmail() {
        // Arrange
        Jwt jwt = createTestJwt("user123", "test@example.com", List.of("USER"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        // Act
        String email = jwtUtil.extractEmail(authentication);

        // Assert
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void extractRoles_ShouldReturnRolesList() {
        // Arrange
        Jwt jwt = createTestJwt("user123", "test@example.com", List.of("USER", "ADMIN"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        // Act
        List<String> roles = jwtUtil.extractRoles(authentication);

        // Assert
        assertThat(roles).containsExactly("USER", "ADMIN");
    }

    @Test
    void hasRole_ShouldReturnTrue_WhenRoleExists() {
        // Arrange
        Jwt jwt = createTestJwt("user123", "test@example.com", List.of("USER", "ADMIN"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        // Act
        boolean hasRole = jwtUtil.hasRole(authentication, "ADMIN");

        // Assert
        assertThat(hasRole).isTrue();
    }

    @Test
    void hasRole_ShouldReturnFalse_WhenRoleDoesNotExist() {
        // Arrange
        Jwt jwt = createTestJwt("user123", "test@example.com", List.of("USER"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        // Act
        boolean hasRole = jwtUtil.hasRole(authentication, "ADMIN");

        // Assert
        assertThat(hasRole).isFalse();
    }

    private Jwt createTestJwt(String subject, String email, List<String> roles) {
        Map<String, Object> claims = Map.of(
            "sub", subject,
            "email", email,
            "realm_access", Map.of("roles", roles)
        );

        return new Jwt(
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            claims
        );
    }
}
