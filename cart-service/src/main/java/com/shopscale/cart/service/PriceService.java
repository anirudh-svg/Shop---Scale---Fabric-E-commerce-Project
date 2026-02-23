package com.shopscale.cart.service;

import com.shopscale.cart.dto.PriceResponse;
import com.shopscale.cart.exception.PriceServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class PriceService {

    private static final Logger logger = LoggerFactory.getLogger(PriceService.class);

    private final WebClient webClient;

    public PriceService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://price-service").build();
    }

    /**
     * Get product price from Price Service with circuit breaker protection
     * Circuit breaker will open after 50% failure rate with minimum 5 calls
     * Time limiter will timeout after 3 seconds
     */
    @CircuitBreaker(name = "priceService", fallbackMethod = "getFallbackPrice")
    @TimeLimiter(name = "priceService")
    public CompletableFuture<PriceResponse> getProductPrice(String productId) {
        logger.info("Fetching price for product: {}", productId);

        return CompletableFuture.supplyAsync(() -> {
            PriceResponse response = webClient.get()
                .uri("/api/prices/{productId}", productId)
                .retrieve()
                .bodyToMono(PriceResponse.class)
                .timeout(Duration.ofSeconds(3))
                .block();

            if (response == null) {
                throw new PriceServiceException("Price not found for product: " + productId);
            }

            logger.info("Successfully fetched price for product: {}", productId);
            return response;
        });
    }

    /**
     * Fallback method when Price Service is unavailable or circuit breaker is open
     * This method is called when:
     * - Price Service is down
     * - Request times out
     * - Circuit breaker is in OPEN state
     */
    private CompletableFuture<PriceResponse> getFallbackPrice(String productId, Exception ex) {
        logger.warn("Using fallback price for product: {} due to: {}", productId, ex.getMessage());
        
        PriceResponse fallbackResponse = new PriceResponse(
            productId,
            "Product " + productId,
            BigDecimal.valueOf(99.99),
            "USD"
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
}
