package com.shopscale.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {

    @Mock
    private GatewayFilterChain filterChain;

    private LoggingFilter loggingFilter;

    @BeforeEach
    void setUp() {
        loggingFilter = new LoggingFilter();
    }

    @Test
    void filter_ShouldLogRequestAndResponse() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/api/test")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(filterChain.filter(any(ServerWebExchange.class)))
            .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(loggingFilter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void filter_ShouldHandleUnknownRemoteAddress() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.POST, "/api/orders")
            .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);

        when(filterChain.filter(any(ServerWebExchange.class)))
            .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(loggingFilter.filter(exchange, filterChain))
            .verifyComplete();
    }

    @Test
    void getOrder_ShouldReturnHighestPrecedence() {
        // Assert
        assert loggingFilter.getOrder() == org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
    }
}
