# Payments Service

The Payments Service is responsible for payment authorization, reversal, and management within the Mercury Order System. It publishes domain events using the transactional outbox pattern for reliable event delivery.

## Overview

### Service Details
- **Port**: 8083
- **Technology Stack**: Kotlin, Spring Boot, JPA, Kafka, Redis
- **Database**: PostgreSQL with Flyway migrations (H2 for tests)
- **Event Pattern**: Outbox pattern with `outbox_events` table and scheduled publisher

### Key Features
- Payment authorization and processing
- Payment reversal and refunds
- Event-driven communication via Kafka
- Transactional outbox for reliable event publishing
- Comprehensive REST API for payment operations
- Distributed tracing and metrics

## Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- PostgreSQL 15+
- Apache Kafka 3.5+
- Redis 7+

### Running the Service (WSL)

```bash
cd /mnt/c/Users/Admin/IdeaProjects/mercury-order-system

# Start infrastructure (Kafka, Redis, Postgres recommended)
docker-compose up -d postgres redis kafka zookeeper

# Run service
./gradlew :services:payments:bootRun
```

### Run with Profile
```bash
./gradlew :services:payments:bootRun -Dspring.profiles.active=dev
```

### WSL Tips
- Build inside WSL at `/mnt/c/...` path to avoid Windows file lock issues
- udev/lsb-release warnings are harmless and can be ignored
- If Gradle daemon crashes, rebuild with `--no-daemon`

## API Endpoints

### Health & Monitoring
- `GET /actuator/health` - Health check endpoint
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

### Payment Management
- `GET /api/v1/payments/{paymentId}` - Get payment details
- `GET /api/v1/payments/{paymentId}/status` - Get payment status
- `GET /api/v1/payments/order/{orderId}` - Get payments by order ID
- `POST /api/v1/payments` - Create and authorize new payment
- `POST /api/v1/payments/{paymentId}/authorize` - Authorize existing payment
- `POST /api/v1/payments/{paymentId}/reverse` - Reverse/refund payment
- `GET /api/v1/payments` - List all payments (with optional status filter)

### Example: Create Payment Request
```json
{
  "orderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amount": 99.99,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD"
}
```

### Example: Reverse Payment Request
```json
{
  "reason": "Customer requested refund"
}
```

## Event-Driven Architecture

### Published Events
The service publishes events to Kafka:

- **`PaymentAuthorizedEvent`** - Payment successfully authorized
- **`PaymentDeclinedEvent`** - Payment authorization failed
- **`PaymentReversedEvent`** - Payment successfully reversed/refunded

### Consumed Events
The service listens to:

- **`OrderCreatedEvent`** - Triggers payment authorization flow

### Event Topics
- Publishes to: `payment-events`
- Consumes from: `order-events`

## Database Schema

### Payments Table
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(32) NOT NULL,
    payment_method VARCHAR(100),
    external_payment_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);
```

### Payment Attempts Table
```sql
CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    external_response TEXT,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_attempts_payment 
        FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);
```

### Outbox Events Table
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    event_data TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT NULL
);
```

## Payment State Machine

Payments follow a defined state transition model:

```
PENDING → AUTHORIZED
   ↓           ↓
DECLINED    REVERSED
```

### State Transitions
- **PENDING → AUTHORIZED**: Successful payment authorization
- **PENDING → DECLINED**: Payment authorization failed
- **AUTHORIZED → REVERSED**: Payment refunded/reversed
- **DECLINED** and **REVERSED** are terminal states

## Configuration

### Environment Variables

#### Database Configuration
- `DB_USERNAME`: Database username (default: payments_user)
- `DB_PASSWORD`: Database password (default: payments_pass)
- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)

#### Kafka Configuration
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka brokers (default: localhost:9092)

#### Redis Configuration
- `REDIS_HOST`: Redis host (default: localhost)
- `REDIS_PORT`: Redis port (default: 6379)

#### Observability
- `SPRING_PROFILES_ACTIVE`: Active profile (default, dev, test, prod)
- `OTLP_ENDPOINT`: OpenTelemetry collector endpoint (default: http://localhost:4317)

#### Outbox Configuration
- `OUTBOX_POLLING_INTERVAL`: Polling interval in ms (default: 5000)
- `OUTBOX_MAX_RETRIES`: Maximum retry attempts (default: 3)
- `OUTBOX_BATCH_SIZE`: Batch size for processing (default: 100)

### Application Configuration
Settings are managed in `src/main/resources/application.yml` including:
- Database connection settings
- Kafka producer/consumer configuration
- Redis connection parameters
- Observability and tracing settings

## Testing

### Running Tests
```bash
# Run all tests
./gradlew :services:payments:test

# Run tests with coverage
./gradlew :services:payments:test jacocoTestReport

# Run integration tests
./gradlew :services:payments:integrationTest
```

### Test Categories
- **Unit Tests**: Business logic and domain model tests
- **Integration Tests**: Database and Kafka integration tests
- **Contract Tests**: API contract verification

## Docker Deployment

### Build Image
```bash
docker build -f services/payments/Dockerfile -t mercury-payments-service .
```

### Run Container
```bash
docker run -p 8083:8083 \
  -e DB_HOST=postgres \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e REDIS_HOST=redis \
  mercury-payments-service
```

### Docker Compose
```bash
docker-compose up -d payments-service
```

## Monitoring & Observability

### Metrics
The service exposes metrics via Prometheus:
- Payment authorization rates
- Payment success/failure rates
- Payment processing duration
- JVM metrics (memory, GC, threads)
- Database connection pool metrics
- Kafka consumer/producer metrics

### Distributed Tracing
- OpenTelemetry integration enabled
- Traces exported to Jaeger
- Correlation IDs for request tracking
- Cross-service trace propagation

### Health Checks
Multiple health indicators:
- Database connectivity
- Kafka connectivity
- Redis connectivity
- Disk space
- Custom payment service health checks

### Logging
Structured logging with:
- JSON format for production
- Correlation IDs for request tracing
- Configurable log levels
- Error tracking and monitoring

## Security

### Payment Security
- Sensitive data handling
- PCI-DSS compliance considerations
- Payment tokenization support
- Secure external payment gateway integration

### API Security
- Input validation for all endpoints
- SQL injection prevention via JPA
- XSS protection headers
- CORS configuration
- Rate limiting (via API Gateway)

### Container Security
- Non-root user execution
- Minimal base image
- Security scanning in CI/CD
- Secret management via environment variables

## Development

### Code Style
- Kotlin coding conventions
- ktlint for code formatting
- Detekt for static analysis
- SonarQube for code quality

### IDE Setup
- IntelliJ IDEA recommended
- Kotlin plugin enabled
- Spring Boot plugin
- Database plugin for schema management

## Troubleshooting (WSL)

### Common Issues
- **udev/lsb-release warnings**: Harmless in WSL. Optionally install: `sudo apt-get install -y libudev-dev lsb-release`
- **Gradle daemon crashes**: Rebuild with `--no-daemon` flag
- **Database connection issues**: Ensure PostgreSQL is running and accessible
- **Kafka connection issues**: Verify Kafka broker is running and ports are exposed

### Debug Mode
```bash
./gradlew :services:payments:bootRun --debug
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/payment-feature`)
3. Commit your changes (`git commit -m 'Add payment feature'`)
4. Push to the branch (`git push origin feature/payment-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## Support

For questions and support:
- Create an issue in the repository
- Contact the development team
- Check the project documentation in `/docs`
