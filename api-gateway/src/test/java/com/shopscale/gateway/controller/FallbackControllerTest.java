package com.shopscale.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController fallbackController = new FallbackController();

    @Test
    void orderServiceFallback_ShouldReturnServiceUnavailable() {
        // Act
        ResponseEntity<Map<String, Object>> response = fallbackController.orderServiceFallback();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(503);
        assertThat(response.getBody().get("error")).isEqualTo("Service Unavailable");
        assertThat(response.getBody().get("message")).asString()
            .contains("Order Service is temporarily unavailable");
    }

    @Test
    void productServiceFallback_ShouldReturnServiceUnavailable() {
        // Act
        ResponseEntity<Map<String, Object>> response = fallbackController.productServiceFallback();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).asString()
            .contains("Product Service is temporarily unavailable");
    }

    @Test
    void inventoryServiceFallback_ShouldReturnServiceUnavailable() {
        // Act
        ResponseEntity<Map<String, Object>> response = fallbackController.inventoryServiceFallback();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).asString()
            .contains("Inventory Service is temporarily unavailable");
    }
}
