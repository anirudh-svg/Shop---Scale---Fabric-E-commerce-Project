# Week 3 - ShopScale Fabric Platform Development

## Overview
Week 3 focused on implementing three critical microservices: API Gateway with security and rate limiting (Task 5), Cart Service with circuit breaker pattern (Task 6), and Notification Service with retry logic (Task 7). These services complete the core business functionality of the ShopScale platform with robust resilience patterns and security features.

---

## Task 5: API Gateway with Security and Rate Limiting

### 5.1 Spring Cloud Gateway Routing Configuration

**Implemented Components:**
- Spring Cloud Gateway application with reactive routing
- Route definitions for all microservices
- Service discovery integration with Eureka
- Load balancing and failover configuration

**Key Features:**
- Dynamic routing based on service discovery
- Path-based routing with predicates
- URI rewriting and path manipulation
- Automatic load balancing across service instances

**Route Configuration:**
- Order Service: /api/orders/** -> order-service
- Product Service: /api/products/** -> product-service
- Inventory Service: /api/inventory/** -> inventory-service
- Cart Service: /api/cart/** -> cart-service
- Notification Service: /api/notifications/** -> notification-service

**Files Created:**
- `api-gateway/src/main/java/com/shopscale/gateway/ApiGatewayApplication.java`
- `api-gateway/src/main/java/com/shopscale/gateway/config/GatewayConfig.java`
- `api-gateway/src/main/resources/application.yml`

---

### 5.2 JWT Authentication Filter

**Implemented Components:**
- `JwtAuthenticationFilter` for token validation
- `JwtUtil` for JWT parsing and validation
- `SecurityConfig` for security configuration
- Token extraction from Authorization header

**Key Features:**
- Bearer token authentication
- JWT signature validation
- Token expiration checking
- Claims extraction (username, roles)
- Integration with downstream services

**Security Flow:**
1. Extract JWT from Authorization header
2. Validate token signature and expiration
3. Extract user claims
4. Add user context to request headers
5. Forward to downstream service

**Files Created:**
- `api-gateway/src/main/java/com/shopscale/gateway/filter/JwtAuthenticationFilter.java`
- `api-gateway/src/main/java/com/shopscale/gateway/util/JwtUtil.java`
- `api-gateway/src/main/java/com/shopscale/gateway/config/SecurityConfig.java`

---

### 5.3 Rate Limiting with Redis

**Implemented Components:**
- `RateLimitConfig` with Redis-based rate limiter
- `RateLimitResponseFilter` for rate limit responses
- `RedisConfig` for Redis connection
- IP-based rate limiting (100 requests/minute)

**Key Features:**
- Token bucket algorithm implementation
- Redis-backed distributed rate limiting
- Per-IP address rate limiting
- Custom rate limit exceeded responses
- Configurable rate limits per route

**Rate Limiting Strategy:**
- Default: 100 requests per minute per IP
- Sliding window implementation
- Distributed across gateway instances
- HTTP 429 (Too Many Requests) response

**Files Created:**
- `api-gateway/src/main/java/com/shopscale/gateway/config/RateLimitConfig.java`
- `api-gateway/src/main/java/com/shopscale/gateway/filter/RateLimitResponseFilter.java`
- `api-gateway/src/main/java/com/shopscale/gateway/config/RedisConfig.java`

---

### 5.4 Additional Gateway Features

**Implemented Components:**
- `LoggingFilter` for request/response logging
- `FallbackController` for circuit breaker fallbacks
- `GlobalExceptionHandler` for error handling

**Key Features:**
- Request/response logging with correlation IDs
- Circuit breaker fallback endpoints
- Centralized exception handling
- CORS configuration
- Health check endpoints

**Files Created:**
- `api-gateway/src/main/java/com/shopscale/gateway/filter/LoggingFilter.java`
- `api-gateway/src/main/java/com/shopscale/gateway/controller/FallbackController.java`
- `api-gateway/src/main/java/com/shopscale/gateway/exception/GlobalExceptionHandler.java`

---

### 5.5 API Gateway Testing

**Test Coverage:**
- JWT utility tests: 5 tests
- Rate limit configuration tests: 3 tests
- Logging filter tests: 4 tests
- Fallback controller tests: 3 tests
- Total: 15 tests (all passing)

**Test Results:**
```
mvn test -pl api-gateway
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

---

## Task 6: Cart Service with Circuit Breaker Pattern

### 6.1 Cart Domain Models and Service Layer

**Implemented Components:**
- `Cart` and `CartItem` domain entities
- `CartRepository` with H2/PostgreSQL support
- `CartService` with business logic
- In-memory cart management

**Key Features:**
- Shopping cart CRUD operations
- Cart item management (add, update, remove)
- Total price calculation
- Cart expiration handling
- Session-based cart tracking

**Domain Model:**
```java
Cart {
  - cartId: String (UUID)
  - customerId: String
  - items: List<CartItem>
  - totalAmount: BigDecimal
  - createdAt/updatedAt: LocalDateTime
}

CartItem {
  - productId: String
  - productName: String
  - quantity: Integer
  - unitPrice: BigDecimal
  - totalPrice: BigDecimal
}
```

**Files Created:**
- `cart-service/src/main/java/com/shopscale/cart/domain/Cart.java`
- `cart-service/src/main/java/com/shopscale/cart/domain/CartItem.java`
- `cart-service/src/main/java/com/shopscale/cart/repository/CartRepository.java`
- `cart-service/src/main/java/com/shopscale/cart/service/CartService.java`

---

### 6.2 Price Service Client with Circuit Breaker

**Implemented Components:**
- `PriceService` with WebClient
- Resilience4j circuit breaker configuration
- Fallback methods for pricing failures
- `WebClientConfig` for reactive HTTP client

**Key Features:**
- Circuit breaker pattern with Resilience4j
- Fallback to cached prices on failure
- Timeout configuration (2 seconds)
- Retry mechanism (3 attempts)
- Bulkhead for resource isolation

**Circuit Breaker Configuration:**
- Failure rate threshold: 50%
- Wait duration in open state: 10 seconds
- Sliding window size: 10 calls
- Minimum number of calls: 5
- Slow call duration threshold: 2 seconds

**Files Created:**
- `cart-service/src/main/java/com/shopscale/cart/service/PriceService.java`
- `cart-service/src/main/java/com/shopscale/cart/config/WebClientConfig.java`

---

### 6.3 Circuit Breaker Monitoring and Logging

**Implemented Components:**
- `CircuitBreakerEventListener` for state transitions
- `PriceServiceHealthIndicator` for health checks
- `MetricsConfig` for circuit breaker metrics
- Actuator integration for monitoring

**Key Features:**
- Circuit breaker state change logging
- Health indicator for Price Service connectivity
- Metrics collection (success rate, failure rate, call duration)
- Actuator endpoints for monitoring
- Event-driven state transition notifications

**Monitored Events:**
- Circuit breaker state transitions (CLOSED, OPEN, HALF_OPEN)
- Success and failure calls
- Slow calls detection
- Fallback executions

**Files Created:**
- `cart-service/src/main/java/com/shopscale/cart/config/CircuitBreakerEventListener.java`
- `cart-service/src/main/java/com/shopscale/cart/health/PriceServiceHealthIndicator.java`
- `cart-service/src/main/java/com/shopscale/cart/config/MetricsConfig.java`

---

### 6.4 Cart Service REST API

**Implemented Components:**
- `CartController` with REST endpoints
- DTOs: `AddToCartRequest`, `CartResponse`, `CartItemResponse`
- `CartMapper` for entity-DTO conversions
- `GlobalExceptionHandler` for error handling

**REST Endpoints (Port: 8084):**
- `POST /api/cart` - Create new cart
- `GET /api/cart/{cartId}` - Get cart by ID
- `POST /api/cart/{cartId}/items` - Add item to cart
- `PUT /api/cart/{cartId}/items/{productId}` - Update cart item
- `DELETE /api/cart/{cartId}/items/{productId}` - Remove cart item
- `DELETE /api/cart/{cartId}` - Clear cart
- `GET /api/cart/customer/{customerId}` - Get customer cart

**Files Created:**
- `cart-service/src/main/java/com/shopscale/cart/controller/CartController.java`
- `cart-service/src/main/java/com/shopscale/cart/dto/AddToCartRequest.java`
- `cart-service/src/main/java/com/shopscale/cart/dto/CartResponse.java`
- `cart-service/src/main/java/com/shopscale/cart/dto/CartItemResponse.java`
- `cart-service/src/main/java/com/shopscale/cart/mapper/CartMapper.java`
- `cart-service/src/main/java/com/shopscale/cart/exception/GlobalExceptionHandler.java`

---

### 6.5 Cart Service Testing

**Test Coverage:**
- Cart service tests: 10 tests
- Price service tests: 8 tests
- Circuit breaker integration tests: 6 tests
- Total: 24 tests (all passing)

**Test Results:**
```
mvn test -pl cart-service
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

---

## Task 7: Notification Service with Retry Logic

### 7.1 Notification Domain Models and Email Service

**Implemented Components:**
- `NotificationHistory` domain model
- `NotificationHistoryService` for audit trail
- `EmailService` with JavaMailSender
- Email template system

**Key Features:**
- Email notification sending
- Notification history tracking
- Template-based email content
- Status tracking (SENT, FAILED, PENDING)
- Audit trail for all notifications

**Domain Model:**
```java
NotificationHistory {
  - id: Long
  - orderId: String
  - recipientEmail: String
  - subject: String
  - content: String
  - status: NotificationStatus
  - attemptCount: Integer
  - sentAt: LocalDateTime
  - createdAt: LocalDateTime
}
```

**Files Created:**
- `notification-service/src/main/java/com/shopscale/notification/domain/NotificationHistory.java`
- `notification-service/src/main/java/com/shopscale/notification/domain/NotificationStatus.java`
- `notification-service/src/main/java/com/shopscale/notification/service/NotificationHistoryService.java`
- `notification-service/src/main/java/com/shopscale/notification/service/EmailService.java`

---

### 7.2 Kafka Event Consumer for Notifications

**Implemented Components:**
- `OrderEventListener` with @KafkaListener
- `KafkaConfig` for consumer configuration
- Event models: `OrderPlacedEvent`, `OrderItemEvent`
- Asynchronous event processing

**Key Features:**
- Consumes from order-placed topic
- Automatic email notification on order placement
- Error handling with retry mechanism
- Manual acknowledgment for reliability
- Dead letter queue for failed messages

**Consumer Configuration:**
- Group ID: notification-service-group
- Auto-offset reset: earliest
- Concurrency: 3 consumers
- Manual commit mode

**Files Created:**
- `notification-service/src/main/java/com/shopscale/notification/listener/OrderEventListener.java`
- `notification-service/src/main/java/com/shopscale/notification/config/KafkaConfig.java`
- `notification-service/src/main/java/com/shopscale/notification/event/OrderPlacedEvent.java`
- `notification-service/src/main/java/com/shopscale/notification/event/OrderItemEvent.java`

---

### 7.3 Retry Logic and Failure Handling

**Implemented Components:**
- `RetryConfig` with Spring Retry
- Exponential backoff strategy
- `DeadLetterQueueService` for failed notifications
- Maximum retry attempts configuration

**Key Features:**
- Spring Retry with @Retryable annotation
- Exponential backoff (initial: 1s, multiplier: 2.0, max: 10s)
- Maximum 3 retry attempts
- Dead letter queue for permanently failed notifications
- Notification status tracking through retry attempts

**Retry Configuration:**
- Initial delay: 1 second
- Backoff multiplier: 2.0
- Maximum delay: 10 seconds
- Maximum attempts: 3
- Recoverable exceptions: MailSendException

**Dead Letter Queue:**
- In-memory storage for failed notifications
- REST API for DLQ management
- Retry capability for DLQ items
- Monitoring and alerting support

**Files Created:**
- `notification-service/src/main/java/com/shopscale/notification/config/RetryConfig.java`
- `notification-service/src/main/java/com/shopscale/notification/service/DeadLetterQueueService.java`
- `notification-service/src/main/java/com/shopscale/notification/controller/DeadLetterQueueController.java`

---

### 7.4 Notification Service REST API

**Implemented Components:**
- `NotificationController` for notification management
- `DeadLetterQueueController` for DLQ operations
- DTOs for request/response handling
- `GlobalExceptionHandler` for error handling

**REST Endpoints (Port: 8085):**
- `GET /api/notifications/history/{orderId}` - Get notification history
- `GET /api/notifications/history` - Get all notifications
- `GET /api/notifications/dlq` - Get dead letter queue
- `POST /api/notifications/dlq/{id}/retry` - Retry failed notification
- `DELETE /api/notifications/dlq/{id}` - Remove from DLQ

**Files Created:**
- `notification-service/src/main/java/com/shopscale/notification/controller/NotificationController.java`
- `notification-service/src/main/java/com/shopscale/notification/controller/DeadLetterQueueController.java`
- `notification-service/src/main/java/com/shopscale/notification/exception/GlobalExceptionHandler.java`
- `notification-service/src/main/java/com/shopscale/notification/exception/EmailSendingException.java`

---

### 7.5 Notification Service Testing

**Test Coverage:**
- Email service tests: 4 tests
- Notification history service tests: 5 tests
- Dead letter queue service tests: 5 tests
- Order event listener tests: 3 tests
- Application context test: 1 test
- Total: 18 tests (all passing)

**Test Results:**
```
mvn test -pl notification-service
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
```

**Key Test Scenarios:**
- Successful email sending
- Retry on failure with eventual success
- Dead letter queue after max retries
- Email content validation
- Notification history tracking

---

## Technical Achievements

### API Gateway
- Spring Cloud Gateway with reactive routing
- JWT authentication and authorization
- Redis-based distributed rate limiting
- Circuit breaker fallback handling
- Comprehensive logging and monitoring

### Cart Service
- Resilience4j circuit breaker pattern
- WebClient for reactive HTTP calls
- Fallback mechanisms for service failures
- Health indicators for dependency monitoring
- Circuit breaker state transition logging

### Notification Service
- Spring Retry with exponential backoff
- Dead letter queue for failed notifications
- Kafka event-driven notifications
- Email notification with templates
- Comprehensive audit trail

---

## Build and Test Results

### API Gateway
```
mvn test -pl api-gateway
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
- JwtUtilTest: 5 passed
- RateLimitConfigTest: 3 passed
- LoggingFilterTest: 4 passed
- FallbackControllerTest: 3 passed
```

### Cart Service
```
mvn test -pl cart-service
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
- CartServiceTest: 10 passed
- PriceServiceTest: 8 passed
- CircuitBreakerIntegrationTest: 6 passed
```

### Notification Service
```
mvn test -pl notification-service
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
- EmailServiceTest: 4 passed
- NotificationHistoryServiceTest: 5 passed
- DeadLetterQueueServiceTest: 5 passed
- OrderEventListenerTest: 3 passed
- NotificationServiceApplicationTest: 1 passed
```

---

## Dependencies Added

### API Gateway
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka Client
- Spring Data Redis Reactive
- JWT libraries (jjwt-api, jjwt-impl, jjwt-jackson)
- Spring Boot Actuator

### Cart Service
- Spring Data JPA
- H2 Database (for testing)
- Spring WebFlux (WebClient)
- Resilience4j Spring Boot 3
- Spring Boot Actuator
- Embedded Redis for testing

### Notification Service
- Spring Kafka
- Spring Mail
- Spring Retry
- GreenMail (for email testing)
- Embedded Kafka for testing
- H2 Database (for testing)

---

## Configuration Highlights

### API Gateway Configuration
```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100
            redis-rate-limiter.burstCapacity: 200
```

### Cart Service Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      priceService:
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
```

### Notification Service Configuration
```yaml
spring:
  retry:
    max-attempts: 3
    initial-interval: 1000
    multiplier: 2.0
    max-interval: 10000
```

---

## Next Steps (Week 4)

1. Task 8: Set up distributed tracing with Zipkin
2. Task 9: Create React frontend application
3. Task 10: Complete Docker deployment and integration

---

## Notes

- All services are production-ready with comprehensive test coverage
- Resilience patterns (circuit breaker, retry) are fully implemented
- Security layer (JWT authentication, rate limiting) is operational
- Event-driven communication is working across all services
- Services are ready for Docker deployment and integration testing
