# ShopScale Fabric Platform

A production-grade, cloud-native e-commerce platform engineered for extreme scalability and resilience. Built on microservices architecture with event-driven communication, the platform maintains high availability during traffic spikes such as Black Friday sales and flash events.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Services Overview](#services-overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Deployment](#deployment)
- [Monitoring and Observability](#monitoring-and-observability)
- [Security](#security)
- [Project Structure](#project-structure)
- [Development Guidelines](#development-guidelines)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

ShopScale Fabric is an enterprise-grade e-commerce platform that demonstrates modern cloud-native architecture patterns and best practices. The system is designed to handle high-traffic scenarios while maintaining data consistency, system resilience, and operational excellence.

The platform implements a distributed microservices architecture where services communicate through both synchronous REST APIs and asynchronous event streams. This hybrid approach ensures immediate user feedback for critical operations while enabling eventual consistency for non-blocking workflows.

### Design Principles

- **Resilience First**: Circuit breakers, retry logic, and fallback mechanisms prevent cascading failures
- **Asynchronous by Default**: Non-critical operations execute asynchronously to maintain system responsiveness
- **Observability Built-In**: Distributed tracing, metrics, and structured logging across all services
- **Security in Depth**: JWT authentication, rate limiting, and encrypted communication channels
- **Scalability**: Stateless services with horizontal scaling capabilities and distributed caching

## Key Features

- **Event-Driven Architecture**: Apache Kafka enables reliable, ordered event processing across services
- **Service Discovery**: Eureka Server provides dynamic service registration and discovery
- **API Gateway**: Single entry point with authentication, rate limiting, and intelligent routing
- **Circuit Breaker Pattern**: Resilience4j protects against cascading failures with automatic fallback
- **Distributed Caching**: Redis reduces database load and improves response times
- **Virtual Threads**: Java 21 Project Loom enables high-concurrency with simplified code
- **Containerized Deployment**: Docker Compose orchestrates the entire platform stack
- **Distributed Tracing**: Zipkin provides end-to-end request visibility across services

## Architecture

### System Architecture

The platform follows a layered microservices architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Client Layer (React SPA)                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              API Gateway (Spring Cloud Gateway)                  │
│  • JWT Authentication    • Rate Limiting    • Request Routing   │
└──────┬──────────────────┬──────────────────┬───────────────────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Order     │    │   Product   │    │    Cart     │
│  Service    │    │   Service   │    │   Service   │
│ (PostgreSQL)│    │  (MongoDB)  │    │ (H2/Memory) │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       │                  │                  │
       ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Apache Kafka Event Bus                        │
│              (OrderPlacedEvent, InventoryUpdatedEvent)          │
└──────┬──────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────┐    ┌─────────────┐
│  Inventory  │    │Notification │
│   Service   │    │   Service   │
│ (PostgreSQL)│    │   (SMTP)    │
└─────────────┘    └─────────────┘

Infrastructure Services:
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Eureka    │  │   Config    │  │    Redis    │  │   Zipkin    │
│   Server    │  │   Server    │  │   Cache     │  │  Tracing    │
└─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘
```

### Communication Patterns

**Synchronous Communication (REST)**
- Client to API Gateway: HTTPS with JWT tokens
- API Gateway to Services: HTTP with service discovery
- Inter-service calls: REST with circuit breaker protection

**Asynchronous Communication (Events)**
- Order Service publishes OrderPlacedEvent to Kafka
- Inventory Service consumes events and updates stock levels
- Notification Service consumes events and sends confirmations
- Event replay capability for reliability and recovery

**Service Discovery**
- All services register with Eureka Server on startup
- API Gateway resolves service locations dynamically
- Health checks ensure only healthy instances receive traffic

**Resilience Patterns**
- Circuit Breaker: Prevents cascading failures (Cart → Price Service)
- Retry Logic: Exponential backoff for transient failures
- Fallback Methods: Graceful degradation when services unavailable
- Timeout Protection: Time limiters prevent indefinite waits

## Technology Stack

### Backend Technologies

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Runtime | Java | 21 | Virtual Threads (Project Loom) for high concurrency |
| Framework | Spring Boot | 3.3 | Microservices foundation |
| API Gateway | Spring Cloud Gateway | 4.x | Routing, authentication, rate limiting |
| Security | Spring Security | 6.x | OAuth2/OIDC, JWT validation |
| Persistence | Spring Data JPA | 3.x | Relational data access |
| Document Store | Spring Data MongoDB | 4.x | NoSQL data access |
| Resilience | Resilience4j | 2.x | Circuit breakers, retry, rate limiting |
| Messaging | Spring Kafka | 3.x | Event-driven communication |

### Data Layer

| Component | Technology | Version | Use Case |
|-----------|-----------|---------|----------|
| Relational DB | PostgreSQL | 15 | Orders, inventory (ACID transactions) |
| Document DB | MongoDB | 7 | Product catalog (flexible schema) |
| Cache | Redis | 7 | Distributed caching, rate limiting, sessions |
| Message Broker | Apache Kafka | 3.x | Event streaming, async communication |
| Coordination | Apache Zookeeper | 3.x | Kafka cluster coordination |

### Infrastructure

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Service Discovery | Netflix Eureka | 4.x | Dynamic service registration |
| Configuration | Spring Cloud Config | 4.x | Centralized configuration management |
| Tracing | Zipkin | 2.x | Distributed request tracing |
| Tracing Bridge | Micrometer Tracing | 1.x | Observability abstraction |
| Containerization | Docker | 20.10+ | Application packaging |
| Orchestration | Docker Compose | 2.0+ | Multi-container deployment |

### Frontend (Planned)

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | React.js | UI component library |
| State Management | Redux Toolkit / Zustand | Application state |
| Build Tool | Vite / Webpack | Module bundling |
| Language | TypeScript | Type-safe JavaScript |

### Development Tools

- **Build Tool**: Maven 3.8+
- **Migration**: Flyway (database versioning)
- **Testing**: JUnit 5, Mockito, TestContainers
- **Code Quality**: SonarQube (optional)
- **API Documentation**: OpenAPI/Swagger (optional)

## Services Overview

### Infrastructure Services

| Service | Port | Responsibility | Technology |
|---------|------|----------------|------------|
| **Eureka Server** | 8761 | Service discovery and health monitoring | Spring Cloud Netflix Eureka |
| **Config Server** | 8888 | Centralized configuration management | Spring Cloud Config |
| **API Gateway** | 8080 | Authentication, routing, rate limiting | Spring Cloud Gateway |

### Business Services

| Service | Port | Responsibility | Data Store | Status |
|---------|------|----------------|------------|--------|
| **Order Service** | 8081 | Order processing, event publishing | PostgreSQL | Production Ready |
| **Product Service** | 8082 | Product catalog, caching | MongoDB + Redis | Production Ready |
| **Inventory Service** | 8083 | Stock management, event consumption | PostgreSQL | Production Ready |
| **Cart Service** | 8084 | Shopping cart, circuit breaker demo | H2 (in-memory) | Production Ready |
| **Notification Service** | 8085 | Email notifications, retry logic | N/A | In Development |

### Service Details

#### Eureka Server
Provides service discovery capabilities allowing microservices to locate each other without hardcoded URLs. Monitors service health and automatically removes unhealthy instances from the registry.

#### Config Server
Manages application configuration across all environments. Supports file-based and Git-backed configuration with dynamic refresh capabilities.

#### API Gateway
Acts as the single entry point for all client requests. Handles cross-cutting concerns including:
- JWT token validation and authentication
- Rate limiting (100 requests/minute per IP)
- Request routing based on service discovery
- Load balancing across service instances
- Request/response logging and monitoring

#### Order Service
Core business service handling order placement and management:
- Processes orders using Java 21 Virtual Threads for high concurrency
- Persists order data to PostgreSQL with ACID guarantees
- Publishes OrderPlacedEvent to Kafka for downstream processing
- Target response time: Under 2 seconds
- Implements Flyway for database migrations

#### Product Service
Manages the product catalog with performance optimization:
- Stores product data in MongoDB for flexible schema
- Implements Redis caching with 1-hour TTL
- Cache hit response time: Under 10ms
- Supports product search and filtering
- Handles high read volumes efficiently

#### Inventory Service
Asynchronous stock management service:
- Consumes OrderPlacedEvent from Kafka
- Updates inventory levels within 5 seconds of order placement
- Publishes InventoryUpdatedEvent for audit trails
- Supports event replay for reliability
- Maintains data consistency through event ordering

#### Cart Service
Demonstrates resilience patterns in microservices:
- Manages shopping cart operations
- Calls Price Service with Resilience4j circuit breaker
- Implements fallback pricing when Price Service unavailable
- Circuit breaker configuration: 50% failure threshold, 30s wait duration
- Provides circuit breaker metrics and health indicators

#### Notification Service (In Development)
Handles customer notifications with reliability:
- Consumes OrderPlacedEvent from Kafka
- Sends order confirmation emails via SMTP
- Implements Spring Retry with exponential backoff (3 attempts)
- Dead letter queue for permanently failed notifications
- Audit logging for compliance and troubleshooting

## Prerequisites

### Required Software

| Software | Minimum Version | Recommended Version | Purpose |
|----------|----------------|---------------------|---------|
| Java JDK | 21 | 21 | Runtime environment |
| Maven | 3.8 | 3.9+ | Build tool |
| Docker Desktop | 20.10 | Latest | Containerization |
| Docker Compose | 2.0 | Latest | Multi-container orchestration |
| Git | 2.30 | Latest | Version control |

### Optional Software

| Software | Version | Purpose |
|----------|---------|---------|
| Node.js | 18+ | Frontend development |
| npm | 9+ | Frontend package management |
| IntelliJ IDEA | Latest | Java IDE (recommended) |
| Postman | Latest | API testing |

### System Requirements

- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 20GB free space
- **CPU**: 4 cores minimum, 8 cores recommended
- **OS**: Windows 10/11, macOS 11+, or Linux (Ubuntu 20.04+)

### Network Requirements

- Internet connection for downloading dependencies
- Ports available: 8080-8085, 8761, 8888, 5432, 27017, 6379, 9092, 9411

## Quick Start

### Step 1: Clone Repository

```bash
git clone https://github.com/your-org/shopscale-fabric.git
cd shopscale-fabric
```

### Step 2: Build Services

Build all microservices using Maven:

```bash
mvn clean install
```

This command:
- Compiles all Java source code
- Runs unit and integration tests
- Packages services as executable JAR files
- Installs artifacts to local Maven repository

Expected output: `BUILD SUCCESS` for all modules

### Step 3: Start Infrastructure

Launch backing services using Docker Compose:

```bash
docker-compose up -d postgres mongodb redis kafka zookeeper
```

Wait approximately 30 seconds for services to initialize. Verify status:

```bash
docker-compose ps
```

All services should show status as `Up`.

### Step 4: Start Application Services

Launch infrastructure services first:

```bash
docker-compose up -d eureka-server config-server
```

Wait 30 seconds for Eureka and Config Server to be ready, then start business services:

```bash
docker-compose up -d api-gateway order-service product-service inventory-service cart-service
```

### Step 5: Verify Deployment

Check Eureka Dashboard to confirm all services are registered:

```
http://localhost:8761
```

Expected: All services showing status `UP` with green indicators.

Check API Gateway health:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### Step 6: Test the Platform

Create a test order:

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productId": "product-456",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ]
  }'
```

Expected: HTTP 201 Created with order details in response.

### Quick Start Troubleshooting

If services fail to start:

1. Check Docker is running: `docker ps`
2. Verify ports are available: `netstat -an | findstr "8080"`
3. Review service logs: `docker-compose logs [service-name]`
4. Ensure Java 21 is installed: `java -version`
5. Verify Maven build succeeded: Check for `BUILD SUCCESS` messages

## Configuration

### Environment Variables

Each service can be configured using environment variables:

```bash
# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=orderdb
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# MongoDB Configuration
MONGODB_HOST=localhost
MONGODB_PORT=27017
MONGODB_DATABASE=productdb

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Eureka Configuration
EUREKA_SERVER_URL=http://localhost:8761/eureka
```

### Service-Specific Configuration

Configuration files are located in:
- `config-server/src/main/resources/config/` for centralized configs
- Each service's `src/main/resources/application.yml` for local configs

### Circuit Breaker Configuration

Cart Service circuit breaker settings:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      price-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
```

### Rate Limiting Configuration

API Gateway rate limiting:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate-limit-route
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
```

## Testing

### Test Execution

#### Run All Tests

Execute the complete test suite across all services:

```bash
mvn test
```

#### Run Tests for Specific Service

```bash
mvn test -pl order-service
mvn test -pl product-service
mvn test -pl inventory-service
mvn test -pl cart-service
mvn test -pl api-gateway
```

#### Run Tests with Coverage

```bash
mvn test jacoco:report
```

Coverage reports generated in `target/site/jacoco/index.html` for each service.

#### Skip Tests During Build

```bash
mvn clean install -DskipTests
```

### Test Strategy

#### Unit Tests

Focus on individual components in isolation:

- **Service Layer**: Business logic validation with mocked dependencies
- **Repository Layer**: Data access using `@DataJpaTest` and `@DataMongoTest`
- **Event Handlers**: Kafka consumers with embedded Kafka
- **Utilities**: Helper classes and mappers

Example test structure:
```java
@SpringBootTest
class OrderServiceTest {
    @MockBean
    private OrderRepository orderRepository;
    
    @MockBean
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Test implementation
    }
}
```

#### Integration Tests

Validate end-to-end workflows:

- **API Testing**: REST endpoints with `@SpringBootTest` and `TestRestTemplate`
- **Database Integration**: Real database operations with TestContainers
- **Event Flow**: Complete event-driven workflows from producer to consumer
- **Circuit Breaker**: Resilience patterns under failure conditions

Example integration test:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateOrderViaAPI() {
        // Test implementation
    }
}
```

#### Performance Tests

Validate system behavior under load:

- **Load Testing**: JMeter scripts for sustained load
- **Stress Testing**: Peak traffic simulation (Black Friday scenarios)
- **Concurrency Testing**: Virtual Thread performance validation
- **Cache Performance**: Redis response time benchmarks

### Test Coverage by Service

| Service | Unit Tests | Integration Tests | Coverage | Status |
|---------|-----------|-------------------|----------|--------|
| Order Service | 15 tests | 8 tests | 85% | Passing |
| Product Service | 12 tests | 6 tests | 82% | Passing |
| Inventory Service | 10 tests | 7 tests | 88% | Passing |
| API Gateway | 8 tests | 5 tests | 75% | Passing |
| Cart Service | 10 tests | 0 tests | 70% | Passing |
| Config Server | 2 tests | 1 test | 60% | Passing |

### Known Test Limitations

#### Cart Service Circuit Breaker Tests

Some integration tests for circuit breaker functionality are currently disabled due to DNS resolution issues with MockWebServer and WebClient. The tests are documented and marked with `@Disabled` annotation.

**Affected Tests:**
- `PriceServiceTest` (6 tests disabled)
- `CircuitBreakerIntegrationTest` (4 tests disabled)

**Workaround:** Circuit breaker functionality is validated through `CartServiceTest` which mocks the PriceService at the service layer.

**Resolution Options:**
1. Use WireMock instead of MockWebServer
2. Mock WebClient.Builder at Spring context level with `@MockBean`
3. Use `@DynamicPropertySource` to override service URLs at runtime

See test class documentation for detailed explanations and alternative approaches.

### Test Best Practices

1. **Isolation**: Each test should be independent and not rely on execution order
2. **Cleanup**: Use `@AfterEach` to clean up test data and reset state
3. **Naming**: Use descriptive test names that explain the scenario
4. **Assertions**: Use AssertJ for fluent and readable assertions
5. **Test Data**: Use builders or factories for consistent test data creation
6. **Mocking**: Mock external dependencies, use real implementations for internal components

### Continuous Integration

Tests are automatically executed on:
- Pull request creation
- Commits to main branch
- Scheduled nightly builds

CI pipeline configuration (example for GitHub Actions):

```yaml
name: CI Pipeline
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: mvn clean test
```

## Deployment

### Docker Compose Deployment

Start all services:

```bash
docker-compose up -d
```

Stop all services:

```bash
docker-compose down
```

View logs:

```bash
docker-compose logs -f [service-name]
```

### Service Startup Order

1. Infrastructure: Zookeeper, Kafka, PostgreSQL, MongoDB, Redis
2. Discovery: Eureka Server
3. Configuration: Config Server
4. Gateway: API Gateway
5. Business Services: Order, Product, Inventory, Cart, Notification

### Health Checks

All services expose health endpoints:

```bash
curl http://localhost:8081/actuator/health  # Order Service
curl http://localhost:8082/actuator/health  # Product Service
curl http://localhost:8083/actuator/health  # Inventory Service
curl http://localhost:8084/actuator/health  # Cart Service
```

### Scaling Services

Scale a specific service:

```bash
docker-compose up -d --scale order-service=3
```

The API Gateway will automatically load balance across instances via Eureka.

## Monitoring and Observability

### Distributed Tracing

Zipkin UI:
```
http://localhost:9411
```

All requests are traced with unique trace IDs that propagate across all microservices.

### Metrics

Actuator endpoints expose metrics:

```bash
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/prometheus
```

### Circuit Breaker Monitoring

Cart Service circuit breaker status:

```bash
curl http://localhost:8084/actuator/health/circuitBreakers
curl http://localhost:8084/actuator/circuitbreakerevents
```

### Logging

Centralized logging configuration:
- Log level: INFO for production, DEBUG for development
- Circuit breaker state transitions logged at INFO level
- Event processing logged with correlation IDs
- Failed operations logged at ERROR level with stack traces

## Security

### Authentication and Authorization

- JWT token-based authentication
- OAuth2/OIDC integration with Keycloak
- Token validation at API Gateway
- Token relay to downstream services

### Rate Limiting

- 100 requests per minute per IP address
- Redis-backed rate limiting
- HTTP 429 response when limits exceeded
- Configurable per-route limits

### Network Security

- Only API Gateway exposed externally (port 8080)
- Internal services communicate via private network
- Database connections use SSL/TLS
- Sensitive data encrypted at rest

### Data Protection

- PII data handling compliance
- Secure credential management
- Environment-based secrets configuration

## Project Structure

```
shopscale-fabric/
├── api-gateway/              # Spring Cloud Gateway
├── cart-service/             # Shopping cart with circuit breaker
├── config-server/            # Centralized configuration
├── eureka-server/            # Service discovery
├── inventory-service/        # Stock management
├── notification-service/     # Email notifications (in progress)
├── order-service/            # Order processing
├── product-service/          # Product catalog
├── scripts/                  # Database initialization scripts
├── docker-compose.yml        # Container orchestration
├── pom.xml                   # Parent Maven configuration
└── README.md                 # This file
```

### Service Structure

Each service follows a standard structure:

```
service-name/
├── src/
│   ├── main/
│   │   ├── java/com/shopscale/[service]/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── domain/           # Entity models
│   │   │   ├── dto/              # Data transfer objects
│   │   │   ├── event/            # Kafka event models
│   │   │   ├── exception/        # Exception handlers
│   │   │   ├── repository/       # Data access layer
│   │   │   ├── service/          # Business logic
│   │   │   └── Application.java  # Main class
│   │   └── resources/
│   │       ├── application.yml   # Configuration
│   │       └── db/migration/     # Database migrations
│   └── test/
│       ├── java/                 # Test classes
│       └── resources/            # Test configuration
├── Dockerfile
└── pom.xml
```

## API Documentation

### Order Service API

Base URL: `http://localhost:8080/orders`

#### Create Order

```http
POST /orders
Content-Type: application/json

{
  "customerId": "string",
  "items": [
    {
      "productId": "string",
      "quantity": integer,
      "unitPrice": decimal
    }
  ]
}
```

Response: `201 Created`
```json
{
  "orderId": "uuid",
  "customerId": "string",
  "items": [...],
  "totalAmount": decimal,
  "status": "PENDING",
  "createdAt": "timestamp"
}
```

#### Get Order

```http
GET /orders/{orderId}
```

Response: `200 OK` with order details

#### List Orders

```http
GET /orders?page=0&size=20
```

Response: `200 OK` with paginated order list

### Product Service API

Base URL: `http://localhost:8080/products`

#### List Products

```http
GET /products?category=electronics&page=0&size=20
```

Response: `200 OK` with product list

#### Get Product

```http
GET /products/{productId}
```

Response: `200 OK` with product details

#### Create Product

```http
POST /products
Content-Type: application/json

{
  "name": "string",
  "description": "string",
  "price": decimal,
  "category": "string",
  "stockQuantity": integer
}
```

Response: `201 Created` with product details

#### Update Product

```http
PUT /products/{productId}
Content-Type: application/json

{
  "name": "string",
  "price": decimal,
  "stockQuantity": integer
}
```

Response: `200 OK` with updated product

#### Delete Product

```http
DELETE /products/{productId}
```

Response: `204 No Content`

### Cart Service API

Base URL: `http://localhost:8080/cart`

#### Add Item to Cart

```http
POST /cart/items
Content-Type: application/json

{
  "customerId": "string",
  "productId": "string",
  "quantity": integer
}
```

Response: `200 OK` with cart details

#### Get Cart

```http
GET /cart/{customerId}
```

Response: `200 OK` with cart contents

#### Update Cart Item

```http
PUT /cart/items
Content-Type: application/json

{
  "customerId": "string",
  "productId": "string",
  "quantity": integer
}
```

Response: `200 OK` with updated cart

#### Remove Cart Item

```http
DELETE /cart/items/{productId}?customerId={customerId}
```

Response: `204 No Content`

#### Clear Cart

```http
DELETE /cart/{customerId}
```

Response: `204 No Content`

### Inventory Service API

Base URL: `http://localhost:8080/inventory`

#### Get Inventory

```http
GET /inventory/{productId}
```

Response: `200 OK`
```json
{
  "productId": "string",
  "availableQuantity": integer,
  "reservedQuantity": integer,
  "lastUpdated": "timestamp"
}
```

#### Update Inventory

```http
PUT /inventory/{productId}
Content-Type: application/json

{
  "quantity": integer
}
```

Response: `200 OK` with updated inventory

#### Reserve Inventory

```http
POST /inventory/reserve
Content-Type: application/json

{
  "productId": "string",
  "quantity": integer
}
```

Response: `200 OK` on successful reservation

#### Release Inventory

```http
POST /inventory/release
Content-Type: application/json

{
  "productId": "string",
  "quantity": integer
}
```

Response: `200 OK` on successful release

### Common Response Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 204 | No Content | Request successful, no response body |
| 400 | Bad Request | Invalid request format or parameters |
| 404 | Not Found | Resource not found |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server-side error occurred |
| 503 | Service Unavailable | Service temporarily unavailable |

### Error Response Format

All error responses follow a consistent format:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'quantity'",
  "path": "/orders"
}
```

## Development Guidelines

### Local Development Setup

#### Running Individual Services

Each service can run independently for development:

```bash
cd order-service
mvn spring-boot:run
```

Ensure required infrastructure is running:

```bash
docker-compose up -d postgres mongodb redis kafka zookeeper eureka-server
```

#### Hot Reload

Spring Boot DevTools enables automatic restart on code changes. Add to your IDE:

**IntelliJ IDEA:**
1. Settings → Build, Execution, Deployment → Compiler
2. Enable "Build project automatically"
3. Settings → Advanced Settings
4. Enable "Allow auto-make to start even if developed application is currently running"

**VS Code:**
- DevTools works automatically with Spring Boot Dashboard extension

#### Database Migrations

Flyway manages database schema versions. Create new migrations:

```
src/main/resources/db/migration/V{version}__{description}.sql
```

Example: `V2__Add_customer_email_column.sql`

Migrations run automatically on application startup.

### Code Style and Standards

#### Java Code Conventions

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and under 50 lines
- Add JavaDoc for public APIs
- Use constructor injection over field injection
- Prefer immutable objects where possible

#### Package Structure

```
com.shopscale.{service}
├── config/           # Configuration classes
├── controller/       # REST controllers
├── domain/           # Entity models
├── dto/              # Data transfer objects
├── event/            # Kafka event models
├── exception/        # Custom exceptions and handlers
├── mapper/           # DTO-Entity mappers
├── repository/       # Data access layer
└── service/          # Business logic
```

#### Logging Standards

Use SLF4J with structured logging:

```java
@Slf4j
public class OrderService {
    public Order createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        // Implementation
        log.debug("Order created with ID: {}", order.getOrderId());
        return order;
    }
}
```

Log levels:
- **ERROR**: System errors requiring immediate attention
- **WARN**: Unexpected situations that don't prevent operation
- **INFO**: Important business events and state changes
- **DEBUG**: Detailed diagnostic information
- **TRACE**: Very detailed diagnostic information

#### Exception Handling

Use global exception handlers:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### Git Workflow

#### Branch Naming

- Feature: `feature/add-payment-service`
- Bugfix: `bugfix/fix-inventory-calculation`
- Hotfix: `hotfix/security-patch`
- Release: `release/v1.2.0`

#### Commit Messages

Follow conventional commits format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
```
feat(order): add order cancellation endpoint

Implement POST /orders/{id}/cancel endpoint with validation
and event publishing to notify downstream services.

Closes #123
```

```
fix(cart): resolve circuit breaker timeout issue

Increase timeout from 2s to 3s to accommodate slower
price service responses during peak traffic.
```

#### Pull Request Process

1. Create feature branch from `main`
2. Implement changes with tests
3. Ensure all tests pass: `mvn test`
4. Update documentation if needed
5. Create pull request with description
6. Address review comments
7. Squash and merge after approval

### IDE Configuration

#### IntelliJ IDEA

Recommended plugins:
- Lombok
- Spring Boot Assistant
- Docker
- Database Navigator

Code style: Import `intellij-code-style.xml` (if provided)

#### VS Code

Recommended extensions:
- Extension Pack for Java
- Spring Boot Extension Pack
- Docker
- REST Client

### Debugging

#### Remote Debugging

Enable remote debugging for a service:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

Connect debugger to `localhost:5005`

#### Docker Container Debugging

Attach to running container:

```bash
docker exec -it shopscale-order-service bash
```

View logs:

```bash
docker logs -f shopscale-order-service
```

### Performance Profiling

#### JVM Profiling

Use VisualVM or JProfiler to analyze:
- CPU usage
- Memory allocation
- Thread activity
- Garbage collection

#### Application Metrics

Access metrics via Actuator:

```bash
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

### Documentation

#### API Documentation

Use OpenAPI/Swagger annotations:

```java
@Operation(summary = "Create new order", description = "Creates a new order and publishes event")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Order created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid request")
})
@PostMapping("/orders")
public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
    // Implementation
}
```

#### Code Documentation

Document complex logic and business rules:

```java
/**
 * Calculates order total with applicable discounts.
 * 
 * Discount rules:
 * - 10% off for orders over $100
 * - 15% off for orders over $500
 * - Discounts are not cumulative
 * 
 * @param items Order items to calculate total for
 * @return Total amount after discounts
 */
public BigDecimal calculateTotal(List<OrderItem> items) {
    // Implementation
}
```

## Troubleshooting

### Service Not Registering with Eureka

- Check Eureka Server is running: `http://localhost:8761`
- Verify network connectivity between services
- Check service logs for registration errors
- Ensure `eureka.client.service-url.defaultZone` is correctly configured

### Kafka Connection Issues

- Verify Kafka and Zookeeper are running
- Check `KAFKA_BOOTSTRAP_SERVERS` configuration
- Ensure topics are created (auto-creation is enabled by default)
- Review Kafka logs: `docker-compose logs kafka`

### Circuit Breaker Not Opening

- Verify failure threshold is reached (default: 50% failure rate)
- Check minimum number of calls is met (default: 5 calls)
- Review circuit breaker metrics: `/actuator/circuitbreakerevents`
- Ensure Resilience4j is properly configured

### Redis Connection Failures

- Verify Redis is running: `docker-compose ps redis`
- Test connection: `redis-cli ping`
- Check Redis host and port configuration
- Review Redis logs: `docker-compose logs redis`

## Contributing

We welcome contributions to the ShopScale Fabric platform. Please follow these guidelines to ensure a smooth collaboration process.

### How to Contribute

1. **Fork the Repository**
   ```bash
   git clone https://github.com/your-username/shopscale-fabric.git
   cd shopscale-fabric
   ```

2. **Create a Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make Your Changes**
   - Write clean, maintainable code
   - Follow existing code style and conventions
   - Add tests for new functionality
   - Update documentation as needed

4. **Test Your Changes**
   ```bash
   mvn clean test
   ```

5. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat(service): add new feature"
   ```

6. **Push to Your Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

7. **Create a Pull Request**
   - Provide a clear description of changes
   - Reference any related issues
   - Ensure CI checks pass

### Contribution Guidelines

#### Code Quality

- All code must pass existing tests
- New features require corresponding tests
- Maintain or improve code coverage
- Follow Java and Spring Boot best practices
- Use meaningful variable and method names

#### Documentation

- Update README.md for significant changes
- Add JavaDoc for public APIs
- Document configuration changes
- Update API documentation if endpoints change

#### Commit Standards

Follow conventional commits specification:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

#### Pull Request Requirements

- Clear title and description
- All tests passing
- No merge conflicts
- Code review approval
- Updated documentation

### Reporting Issues

When reporting bugs or requesting features:

1. Check existing issues first
2. Use issue templates if available
3. Provide detailed description
4. Include steps to reproduce (for bugs)
5. Specify environment details

### Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on the code, not the person
- Help others learn and grow

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### MIT License Summary

Permission is granted to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the software, subject to the following conditions:

- The above copyright notice and this permission notice shall be included in all copies
- The software is provided "as is", without warranty of any kind

For the full license text, see the LICENSE file in the repository root.

## Contact and Support

### Project Maintainers

For questions, suggestions, or support:

- **Email**: dev-team@shopscale.com
- **Issue Tracker**: [GitHub Issues](https://github.com/your-org/shopscale-fabric/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-org/shopscale-fabric/discussions)

### Community

- **Documentation**: [Wiki](https://github.com/your-org/shopscale-fabric/wiki)
- **Changelog**: [CHANGELOG.md](CHANGELOG.md)
- **Roadmap**: [Project Roadmap](https://github.com/your-org/shopscale-fabric/projects)

### Commercial Support

For enterprise support, training, or consulting services, contact: enterprise@shopscale.com

---

**Built with** Java 21, Spring Boot 3.3, and modern cloud-native technologies.

**Maintained by** the ShopScale development team.
