# Mercury Order System

A comprehensive microservices-based order management system built with Kotlin, Spring Boot, and event-driven architecture.

## 🏗️ Architecture Overview

The Mercury Order System implements a distributed saga pattern with the following microservices:

- **API Gateway** (Port 8080) - Front door with routing, rate limiting, health checks, and request logging
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
- Optional for WSL users: `libudev-dev` and `lsb-release` for cleaner system info (warnings are harmless if missing)

### Quick Start (WSL-friendly)

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
   # From a WSL terminal
   ./gradlew clean build

   # Run all services via Docker Compose (recommended for local integration)
   docker-compose up -d api-gateway orders-service payments-service inventory-service
   ```

4. **Verify the system**
   ```bash
   # Check API Gateway health
   curl http://localhost:8080/health
   
   # Check all services via API Gateway
   curl http://localhost:8080/health/ready
   ```

### Development Mode (WSL / Gradle)

Run services individually for development:

```bash
# In a WSL terminal at repository root

# Terminal 1 - Orders Service
./gradlew :services:orders:bootRun

# Terminal 2 - Payments Service
./gradlew :services:payments:bootRun

# Terminal 3 - Inventory Service
./gradlew :services:inventory:bootRun

# Terminal 4 - API Gateway
./gradlew :services:api-gateway:bootRun
```

To run with a Spring profile (e.g., dev):
```bash
./gradlew :services:orders:bootRun -Dspring.profiles.active=dev
```

## 📡 API Endpoints

### API Gateway (Port 8080)
- `GET /health` - Health check endpoint
- `GET /health/ready` - Readiness probe
- `GET /health/live` - Liveness probe
- `GET /api/orders/{orderId}` - Get order details (routed to Orders service)
- `POST /api/orders` - Create new order (routed to Orders service)
- `GET /api/orders/customer/{customerId}` - Get customer orders (routed to Orders service)
- `POST /api/orders/{orderId}/cancel` - Cancel order (routed to Orders service)
- `GET /api/payments/{paymentId}` - Get payment details (routed to Payments service)
- `GET /api/inventory/{sku}` - Get inventory details (routed to Inventory service)

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

## 🌐 API Gateway Features

The API Gateway provides enterprise-grade capabilities:

### **Routing & Load Balancing**
- Intelligent routing to backend services
- Load balancing across service instances
- Path-based routing with strip prefix

### **Rate Limiting**
- Redis-based rate limiting (10 req/sec, burst of 20)
- Multiple rate limiting strategies:
  - IP-based limiting
  - User-Agent + IP combination
  - User ID-based limiting (for authenticated users)

### **Health Monitoring**
- Health check endpoint (`/health`)
- Readiness probe (`/health/ready`) for Kubernetes
- Liveness probe (`/health/live`) for container orchestration

### **Request Logging**
- Comprehensive request/response logging
- Performance timing metrics
- Error tracking and monitoring

### **CORS Support**
- Pre-configured for frontend applications
- Configurable allowed origins, methods, and headers

### **Observability**
- Prometheus metrics integration
- OpenTelemetry distributed tracing
- Request correlation IDs

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

## 🔍 Read Models and Replay

The Orders service maintains a separate query-optimized read model that is updated by event handlers and can be fully rebuilt via replay.

- Read model table: `order_read_model` (denormalized by `orderId`, includes `customerId`, `status`, `totalAmount`, `currency`, `lastEventType`, timestamps)
- Event-driven projector updates the read model from Kafka topics: `order-events`, `payment-events`, `inventory-events`
- Admin replay trigger rebuilds the read model from earliest offsets

Replay trigger:

```bash
curl -X POST http://localhost:8082/api/v1/admin/read-model/replay
```

Notes:
- Uses a unique consumer group (does not disturb live consumers)
- Idempotent upserts ensure safe reprocessing
- Consider pausing read-model consumers during maintenance for consistent rebuilds

## 📊 Service Status

| Service | Status | Port | Features |
|---------|--------|------|----------|
| **API Gateway** | ✅ **Fully Functional** | 8080 | Routing, Rate Limiting, Health Checks, Logging |
| **Orders Service** | ✅ **Fully Functional** | 8082 | Event Sourcing, Read Models, Saga Management |
| **Payments Service** | 🟡 **Partial** | 8083 | Domain Model, Event Handling (needs API layer) |
| **Inventory Service** | 🟡 **Partial** | 8081 | Domain Model, Metrics (needs API layer) |

### **Legend**
- ✅ **Fully Functional**: Complete with API, business logic, and testing
- 🟡 **Partial**: Core functionality implemented, needs API layer completion

## 🏗️ Project Structure

```
mercury-order-system/
├── common/
│   ├── common-events/          # Shared event definitions
│   └── common-tracing/         # OpenTelemetry configuration
├── services/
│   ├── api-gateway/           # ✅ API Gateway service (fully functional)
│   ├── orders/                # ✅ Orders service (fully functional)
│   ├── payments/              # 🟡 Payments service (partial)
│   └── inventory/             # 🟡 Inventory service (partial)
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

Build individual images locally:
```bash
# Orders
docker build -f services/orders/Dockerfile -t mercury-orders-service .

# Payments
docker build -f services/payments/Dockerfile -t mercury-payments-service .

# Inventory
docker build -f services/inventory/Dockerfile -t mercury-inventory-service .

# API Gateway
docker build -f services/api-gateway/Dockerfile -t mercury-api-gateway .
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
- [Monitoring stack configuration](monitoring/)

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

---

## 🧰 Troubleshooting (WSL/Windows)

### **General Issues**
- Did not find udev library / File not found: `/etc/lsb-release`:
  - Harmless in WSL. Optionally install: `sudo apt-get update && sudo apt-get install -y libudev-dev lsb-release`.
- Gradle daemon crashes or file-lock issues on Windows:
  - Retry with `./gradlew clean build --no-daemon` inside WSL.
  - Ensure the project is built from WSL path (`/mnt/c/...`) and not from Windows PowerShell at the same time.

### **Infrastructure Issues**
- Kafka/Redis/Postgres connection refused:
  - Ensure `docker-compose ps` shows containers healthy and ports exposed.
  - Check service `application.yml` hosts/ports match compose (usually `localhost` in WSL).
- Ports already in use:
  - Stop conflicting processes or change service ports in `application-*.yml`.
- Database migrations fail:
  - Confirm DB containers are up; re-run `./gradlew :services:<svc>:bootRun` after infra is ready.

### **API Gateway Issues**
- API Gateway won't start:
  - Check Redis connectivity: `telnet localhost 6379`
  - Verify port 8080 is available: `netstat -ano | findstr :8080`
- Rate limiting not working:
  - Ensure Redis is running and accessible
  - Check rate limit configuration in `application.yml`
- Service routing failures:
  - Verify backend services are running on correct ports
  - Check routing configuration in gateway `application.yml`
- Health checks failing:
  - Test individual service health endpoints
  - Review gateway health controller logs