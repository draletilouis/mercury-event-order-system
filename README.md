# Mercury Order System

A comprehensive microservices-based order management system built with Kotlin, Spring Boot, and event-driven architecture.

## 🏗️ Architecture Overview

The Mercury Order System implements a distributed saga pattern with the following microservices:

- **API Gateway** (Port 8080) - Front door with idempotency and routing
- **Orders Service** (Port 8082) - Order lifecycle management
- **Payments Service** (Port 8083) - Payment authorization and processing
- **Inventory Service** (Port 8081) - Stock reservation and management

## 🚀 Tech Stack

- **Language**: Kotlin + Spring Boot 3.3.4
- **Database**: PostgreSQL (AWS RDS compatible)
- **Caching**: Redis
- **Messaging**: Apache Kafka
- **Infrastructure**: Docker, Kubernetes-ready
- **Observability**: OpenTelemetry, Prometheus, Grafana, Jaeger
- **Build Tool**: Gradle with multi-module setup

## 📊 System Flow

### Order Creation Saga
1. **OrderCreated** → Orders service creates order in PENDING status
2. **PaymentAuthorized** → Payments service authorizes payment
3. **InventoryReserved** → Inventory service reserves stock
4. **OrderCompleted** → Orders service marks order as COMPLETED

### Compensation Flow
If any step fails:
- **PaymentDeclined** → Orders service cancels order
- **InventoryInsufficient** → Orders service cancels order
- **PaymentReversed** → Compensate for successful payment
- **InventoryReleased** → Compensate for successful reservation

## 🛠️ Getting Started

### Prerequisites
- Docker and Docker Compose
- JDK 17+
- Gradle 8+

### Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd mercury-order-system
   ```

2. **Start the infrastructure**
   ```bash
   docker-compose up -d postgres redis kafka zookeeper jaeger prometheus grafana
   ```

3. **Build and run services**
   ```bash
   ./gradlew build
   docker-compose up -d api-gateway orders-service payments-service inventory-service
   ```

4. **Verify the system**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Development Mode

Run services individually for development:

```bash
# Terminal 1 - Orders Service
./gradlew :services:orders:bootRun

# Terminal 2 - Payments Service  
./gradlew :services:payments:bootRun

# Terminal 3 - Inventory Service
./gradlew :services:inventory:bootRun

# Terminal 4 - API Gateway
./gradlew :services:api-gateway:bootRun
```

## 📡 API Endpoints

### API Gateway (Port 8080)
- `GET /api/orders/{orderId}` - Get order details
- `POST /api/orders` - Create new order
- `GET /api/orders/customer/{customerId}` - Get customer orders
- `POST /api/orders/{orderId}/cancel` - Cancel order

### Direct Service Access
- **Orders**: http://localhost:8082
- **Payments**: http://localhost:8083  
- **Inventory**: http://localhost:8081

## 🗄️ Database Schema

Each service maintains its own PostgreSQL database:

- **orders_db** - Orders and order items
- **payments_db** - Payments and payment attempts
- **inventory_db** - Inventory items and reservations
- **gateway_db** - Idempotency keys

See [docs/database-schema.md](docs/database-schema.md) for detailed schema information.

## 📈 Observability

### Metrics & Monitoring
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Jaeger**: http://localhost:16686

### Health Checks
All services expose health endpoints:
- `/actuator/health` - Service health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## 🔄 Event Flow

The system uses Apache Kafka for event-driven communication:

### Topics
- `order-events` - Order lifecycle events
- `payment-events` - Payment processing events
- `inventory-events` - Inventory management events

### Event Types
- `OrderCreated`, `OrderCompleted`, `OrderCancelled`
- `PaymentAuthorized`, `PaymentDeclined`, `PaymentReversed`
- `InventoryReserved`, `InventoryInsufficient`, `InventoryReleased`

## 🏗️ Project Structure

```
mercury-order-system/
├── common/
│   ├── common-events/          # Shared event definitions
│   └── common-tracing/         # OpenTelemetry configuration
├── services/
│   ├── api-gateway/           # API Gateway service
│   ├── orders/                # Orders service
│   ├── payments/              # Payments service
│   └── inventory/             # Inventory service
├── docs/                      # Documentation
├── scripts/                   # Database initialization
├── monitoring/                # Observability configs
└── docker-compose.yml         # Infrastructure setup
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Load Testing
```bash
# Example with Apache Bench
ab -n 1000 -c 10 -H "Content-Type: application/json" \
   -p order-request.json http://localhost:8080/api/orders
```

## 🚀 Deployment

### Docker
```bash
docker-compose up -d
```

### Kubernetes
```bash
kubectl apply -f k8s/
```

### AWS EKS
```bash
# Deploy with Terraform
cd terraform/
terraform init
terraform plan
terraform apply
```

## 🔧 Configuration

### Environment Variables
- `DB_USERNAME` / `DB_PASSWORD` - Database credentials
- `REDIS_HOST` / `REDIS_PORT` - Redis connection
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers
- `OTLP_ENDPOINT` - OpenTelemetry collector

### Application Properties
Each service has its own `application.yml` with:
- Database configuration
- Kafka settings
- Redis configuration
- Observability settings

## 📚 Additional Documentation

- [Database Schema](docs/database-schema.md)
- [API Documentation](docs/api-documentation.md)
- [Deployment Guide](docs/deployment.md)
- [Monitoring Setup](docs/monitoring.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions and support:
- Create an issue in the repository
- Check the documentation in the `docs/` folder
- Review the health endpoints for service status