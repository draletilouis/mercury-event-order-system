# Orders Service

The Orders Service is a core microservice in the Mercury Order System, responsible for managing the complete order lifecycle from creation to completion. It implements event-driven architecture with reliable event publishing using the outbox pattern.

## 🏗️ Architecture

### Service Overview
- **Port**: 8082
- **Context Path**: `/api/v1`
- **Language**: Kotlin
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL with Flyway migrations
- **Event Streaming**: Apache Kafka
- **Caching**: Redis
- **Observability**: OpenTelemetry, Prometheus, Jaeger

### Key Components

```
services/orders/
├── src/main/kotlin/com/mercury/orders/orders/
│   ├── config/           # Configuration classes
│   ├── controller/       # REST API controllers
│   ├── domain/          # Domain entities and enums
│   ├── dto/             # Data transfer objects
│   ├── eventhandler/    # Kafka event handlers
│   ├── exception/       # Global exception handling
│   ├── repository/      # Data access layer
│   ├── service/         # Business logic services
│   └── validation/      # Custom validators
├── src/main/resources/
│   ├── db/migration/    # Flyway database migrations
│   └── application*.yml # Configuration files
└── src/test/           # Test classes
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- PostgreSQL 15+
- Apache Kafka 3.5+
- Redis 7+

### Running Locally

#### Option 1: Using Docker Compose (Recommended)
```bash
# Start all infrastructure services
docker-compose up -d postgres redis kafka zookeeper jaeger

# Run the Orders Service
./gradlew :services:orders:bootRun
```

#### Option 2: Using WSL (Development)
```bash
# Use the provided WSL script with H2 in-memory database
./run-orders-wsl.sh
```

#### Option 3: Full Docker Environment
```bash
# Start complete system
docker-compose up -d

# Orders Service will be available at http://localhost:8082
```

### Configuration Profiles

The service supports multiple configuration profiles:

- **`default`**: Production-ready configuration with PostgreSQL
- **`dev`**: Development configuration with enhanced logging
- **`test`**: Test configuration with H2 in-memory database
- **`prod`**: Production configuration with optimizations

### WSL Tips
- Build inside WSL at `/mnt/c/...` path to avoid Windows file lock issues.
- If you see udev/lsb-release warnings, they are safe to ignore.
- Use `--no-daemon` if Gradle daemon crashes: `./gradlew :services:orders:build --no-daemon`.

## 📊 Database Schema

### Core Tables

#### Orders Table
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'PAYMENT_PENDING', 'INVENTORY_PENDING', 'COMPLETED', 'CANCELLED')),
    total_amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);
```

#### Order Items Table
```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

#### Outbox Events Table
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_data TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);
```

### Order State Machine

The order follows a strict state transition model:

```
PENDING → PAYMENT_PENDING → INVENTORY_PENDING → COMPLETED
    ↓              ↓                ↓
CANCELLED     CANCELLED        CANCELLED
```

## 🔌 API Endpoints

### Health & Monitoring
- `GET /api/v1/health` - Health check endpoint
- `GET /api/v1/actuator/health` - Detailed health information
- `GET /api/v1/actuator/metrics` - Application metrics
- `GET /api/v1/actuator/prometheus` - Prometheus metrics

### Order Management
- `POST /api/v1/orders` - Create a new order
- `GET /api/v1/orders/{orderId}` - Get order by ID
- `GET /api/v1/orders/customer/{customerId}` - Get orders by customer
- `PUT /api/v1/orders/{orderId}/cancel` - Cancel an order

### Example: Create Order Request
```json
{
  "customerId": "customer-123",
  "items": [
    {
      "sku": "PROD-001",
      "quantity": 2,
      "unitPrice": 29.99
    },
    {
      "sku": "PROD-002", 
      "quantity": 1,
      "unitPrice": 49.99
    }
  ],
  "currency": "USD"
}
```

## 🎯 Event-Driven Architecture

### Published Events
The service publishes the following events via Kafka:

- **`OrderCreatedEvent`**: When a new order is created
- **`OrderPaymentPendingEvent`**: When order moves to payment processing
- **`OrderInventoryPendingEvent`**: When order moves to inventory check
- **`OrderCompletedEvent`**: When order is successfully completed
- **`OrderCancelledEvent`**: When order is cancelled

### Consumed Events
The service listens to:

- **`PaymentProcessedEvent`**: From Payments Service
- **`PaymentFailedEvent`**: From Payments Service
- **`InventoryReservedEvent`**: From Inventory Service
- **`InventoryReservationFailedEvent`**: From Inventory Service

### Outbox Pattern
The service implements the transactional outbox pattern to ensure reliable event publishing:

1. Events are stored in `outbox_events` table within the same transaction as business data
2. A background scheduler publishes events to Kafka
3. Failed events are retried with exponential backoff
4. Successfully published events are marked as `PUBLISHED`

## ⚙️ Configuration

### Environment Variables

#### Database Configuration
- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: orders_db)
- `DB_USERNAME`: Database username (default: orders_user)
- `DB_PASSWORD`: Database password (default: orders_pass)
- `DB_POOL_SIZE`: Connection pool size (default: 15)

#### Kafka Configuration
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: localhost:9092)
- `KAFKA_GROUP_ID`: Consumer group ID (default: orders-service)

#### Redis Configuration
- `REDIS_HOST`: Redis host (default: localhost)
- `REDIS_PORT`: Redis port (default: 6379)

#### Application Configuration
- `PORT`: Service port (default: 8082)
- `SPRING_PROFILES_ACTIVE`: Active profile (default: default)
- `LOG_LEVEL`: Logging level (default: INFO)

#### Business Rules
- `MAX_ORDER_VALUE`: Maximum order value (default: 100000.00)
- `MAX_ITEMS_PER_ORDER`: Maximum items per order (default: 50)
- `MAX_QUANTITY_PER_ITEM`: Maximum quantity per item (default: 100)

#### Outbox Configuration
- `OUTBOX_POLLING_INTERVAL`: Polling interval in ms (default: 5000)
- `OUTBOX_RETRY_INTERVAL`: Retry interval in ms (default: 30000)
- `OUTBOX_CLEANUP_INTERVAL`: Cleanup interval in ms (default: 3600000)
- `OUTBOX_MAX_RETRIES`: Maximum retry attempts (default: 3)
- `OUTBOX_BATCH_SIZE`: Batch size for processing (default: 100)

## 🧪 Testing

### Running Tests
```bash
# Run all tests
./gradlew :services:orders:test

# Run tests with coverage
./gradlew :services:orders:test jacocoTestReport

# Run integration tests
./gradlew :services:orders:integrationTest
```

### Test Categories
- **Unit Tests**: Domain logic and service layer tests
- **Integration Tests**: Database and Kafka integration tests
- **Contract Tests**: API contract verification
- **Performance Tests**: Load and stress testing

### Test Configuration
Tests use H2 in-memory database and embedded Kafka for fast execution and isolation.

## 🔍 Monitoring & Observability

### Metrics
The service exposes metrics via Prometheus:
- HTTP request metrics (duration, count, errors)
- JVM metrics (memory, GC, threads)
- Database connection pool metrics
- Kafka consumer/producer metrics
- Custom business metrics

### Tracing
Distributed tracing is enabled via OpenTelemetry:
- Traces are exported to Jaeger
- Correlation IDs are propagated across services
- Database and Kafka operations are traced

### Health Checks
Multiple health indicators:
- Database connectivity
- Kafka connectivity
- Redis connectivity
- Disk space
- Custom business health checks

### Logging
Structured logging with:
- JSON format for production
- Correlation IDs for request tracing
- Configurable log levels per package
- Centralized error logging

## 🔒 Security

### Authentication & Authorization
- JWT token validation (when integrated with API Gateway)
- Role-based access control
- Request validation and sanitization

### Data Protection
- Input validation for all API endpoints
- SQL injection prevention via JPA
- XSS protection headers
- CORS configuration

### Container Security
- Non-root user execution
- Minimal base image (Alpine)
- Security scanning in CI/CD
- Secret management via environment variables

## 🚀 Deployment

### Docker Deployment
```bash
# Build the image
docker build -f services/orders/Dockerfile -t mercury-orders-service .

# Run the container
docker run -p 8082:8082 \
  -e DB_HOST=postgres \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e REDIS_HOST=redis \
  mercury-orders-service
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orders-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: orders-service
  template:
    metadata:
      labels:
        app: orders-service
    spec:
      containers:
      - name: orders-service
        image: mercury-orders-service:latest
        ports:
        - containerPort: 8082
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        livenessProbe:
          httpGet:
            path: /api/v1/actuator/health
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/actuator/health
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 5
```

### Production Considerations
- Use external configuration management
- Implement proper secret management
- Set up monitoring and alerting
- Configure log aggregation
- Implement backup strategies
- Set up disaster recovery procedures

## 🛠️ Development

### Code Style
- Kotlin coding conventions
- ktlint for code formatting
- Detekt for static analysis
- SonarQube for code quality

### Git Workflow
- Feature branches from `develop`
- Pull request reviews required
- Automated testing on PRs
- Conventional commit messages

### IDE Setup
- IntelliJ IDEA recommended
- Kotlin plugin enabled
- Spring Boot plugin
- Database plugin for schema management

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## 📞 Support

For questions and support:
- Create an issue in the repository
- Contact the development team
- Check the project documentation in `/docs`

## 🗺️ Roadmap

### Upcoming Features
- [ ] Order modification support
- [ ] Bulk order operations
- [ ] Advanced reporting endpoints
- [ ] Order scheduling capabilities
- [ ] Enhanced validation rules
- [ ] Performance optimizations

### Technical Improvements
- [ ] GraphQL API support
- [ ] Event sourcing implementation
- [ ] CQRS pattern adoption
- [ ] Advanced caching strategies
- [ ] Circuit breaker implementation
- [ ] Rate limiting support











