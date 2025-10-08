# API Gateway Service

The API Gateway serves as the single entry point for all client requests to the Mercury Order System. It provides enterprise-grade features including routing, rate limiting, health monitoring, and observability.

## Features

### Core Functionality
- **Service Routing**: Intelligent routing to backend microservices
- **Load Balancing**: Distribution of requests across service instances
- **Request/Response Logging**: Comprehensive logging with timing metrics
- **Health Monitoring**: Kubernetes-ready health, readiness, and liveness endpoints

### Rate Limiting
- **Redis-based**: Scalable rate limiting with Redis backend
- **Multiple Strategies**:
  - IP-based limiting (`clientIdKeyResolver`)
  - User-Agent + IP combination (`ipUserAgentKeyResolver`)
  - User ID-based limiting (`userIdKeyResolver`)
- **Configuration**: 10 requests/second with burst capacity of 20

### Security & Resilience
- **CORS Support**: Pre-configured for frontend applications
- **Error Handling**: Graceful error handling and fallback mechanisms
- **Circuit Breaker**: Resilience4j integration for fault tolerance

### Observability
- **Prometheus Metrics**: Built-in metrics collection
- **OpenTelemetry Tracing**: Distributed tracing support
- **Health Endpoints**: Multiple health check endpoints for monitoring

## Architecture

```
Client Request → API Gateway → Backend Services
                      ↓
                [Rate Limiting]
                      ↓
                [Request Logging]
                      ↓
                [Service Routing]
                      ↓
              Orders | Payments | Inventory
```

## Service Details

### Technical Stack
- **Port**: 8080
- **Framework**: Spring Cloud Gateway (WebFlux)
- **Language**: Kotlin
- **Cache**: Redis for rate limiting
- **Observability**: OpenTelemetry, Prometheus

### Key Components
- **Configuration**: `IdempotencyProperties`, `RateLimitConfiguration`
- **Filters**: `SimpleLoggingFilter` for request/response logging
- **Controllers**: `HealthController` for health check endpoints
- **Resolvers**: Multiple rate limit key resolvers for different strategies

## Endpoints

### Health Endpoints
- `GET /health` - General health status
- `GET /health/ready` - Readiness probe for Kubernetes
- `GET /health/live` - Liveness probe for container orchestration

### Service Routing
- `/api/orders/**` → Orders Service (Port 8082)
- `/api/payments/**` → Payments Service (Port 8083)
- `/api/inventory/**` → Inventory Service (Port 8081)

### Monitoring
- `GET /actuator/health` - Spring Boot actuator health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Configuration

### Application Properties

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway-service
  
  cloud:
    gateway:
      routes:
        - id: orders-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=2

app:
  idempotency:
    key-header: X-Idempotency-Key
    ttl-hours: 24
```

### Environment Variables
- `REDIS_HOST` - Redis server host (default: localhost)
- `REDIS_PORT` - Redis server port (default: 6379)
- `OTLP_ENDPOINT` - OpenTelemetry collector endpoint (default: http://localhost:4317)

### Rate Limiting Configuration
Configure in `application.yml`:
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            key-resolver: "#{@clientIdKeyResolver}"
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20
```

### CORS Configuration
Configure allowed origins in `application.yml`:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "http://localhost:3000"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

## Running the Service

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Redis 7+

### Development Mode
```bash
./gradlew :services:api-gateway:bootRun
```

### Docker
```bash
# Build image
docker build -f services/api-gateway/Dockerfile -t mercury-api-gateway .

# Run container
docker run -p 8080:8080 \
  -e REDIS_HOST=redis \
  mercury-api-gateway
```

### Docker Compose
```bash
docker-compose up -d api-gateway
```

### WSL Tips
- Build inside WSL at `/mnt/c/...` path to avoid file lock issues
- If port 8080 is in use, change the port or stop conflicting processes
- Use `--no-daemon` flag if Gradle daemon crashes

## Testing

### Health Check
```bash
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Readiness Check
```bash
curl http://localhost:8080/health/ready
```

### Liveness Check
```bash
curl http://localhost:8080/health/live
```

### Service Routing Test
```bash
# This should route to the Orders service
curl http://localhost:8080/api/orders/health

# This should route to the Payments service
curl http://localhost:8080/api/payments/actuator/health

# This should route to the Inventory service
curl http://localhost:8080/api/inventory/health
```

### Rate Limiting Test
```bash
# Send multiple requests to trigger rate limiting
for i in {1..25}; do
  curl -w "\n" http://localhost:8080/api/orders/health
done
```

After exceeding the burst capacity, you should receive HTTP 429 responses.

## Monitoring

### Metrics
The gateway exposes various metrics:
- Request count and duration
- Rate limiting metrics (allowed/rejected requests)
- Error rates and response codes
- Service routing statistics
- Redis connection metrics
- JVM metrics (memory, GC, threads)

### Access Metrics
```bash
# View all available metrics
curl http://localhost:8080/actuator/metrics

# View specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

### Health Checks
Multiple health indicators monitor:
- Service availability
- Redis connectivity
- Backend service health
- Disk space
- Custom health indicators

### Logging
Comprehensive logging includes:
- Request/response logging with timing
- Performance metrics
- Error tracking
- Rate limiting events
- Service routing events

### Distributed Tracing
- OpenTelemetry integration enabled
- Traces exported to Jaeger
- Correlation IDs propagated to backend services
- Request trace spans for routing and filtering

## Security Considerations

### Rate Limiting
- Protects against DDoS attacks
- Prevents API abuse
- Configurable per client/IP
- Redis-based for distributed rate limiting

### CORS
- Pre-configured for localhost:3000 (development)
- Configurable for production domains
- Supports credentials and custom headers

### Future Enhancements
- JWT authentication and validation
- API key management
- Request validation and sanitization
- Security headers (HSTS, CSP, etc.)
- IP whitelisting/blacklisting

## Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Windows - Kill process using port 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/WSL
lsof -ti:8080 | xargs kill -9
```

#### 2. Redis Connection Issues
- Ensure Redis is running: `docker-compose ps redis`
- Check Redis connectivity: `telnet localhost 6379` or `redis-cli ping`
- Verify Redis host/port configuration in `application.yml`
- Check Redis logs: `docker-compose logs redis`

#### 3. Backend Service Unavailable
- Verify backend services are running on correct ports
- Check service health endpoints directly:
  ```bash
  curl http://localhost:8082/actuator/health  # Orders
  curl http://localhost:8083/actuator/health  # Payments
  curl http://localhost:8081/actuator/health  # Inventory
  ```
- Review routing configuration in `application.yml`
- Check gateway logs for routing errors

#### 4. Rate Limiting Not Working
- Verify Redis is running and accessible
- Check rate limit configuration in `application.yml`
- Ensure Redis rate limiter is properly configured
- Review rate limit metrics: `curl http://localhost:8080/actuator/metrics/gateway.requests`

#### 5. CORS Issues
- Verify CORS configuration matches your frontend origin
- Check browser console for CORS errors
- Test with curl using `-H "Origin: http://localhost:3000"`

### Debug Mode
```bash
./gradlew :services:api-gateway:bootRun --debug
```

### View Logs
```bash
# Docker logs
docker-compose logs -f api-gateway

# Specific time range
docker-compose logs --since 10m api-gateway
```

## Dependencies

### Core Dependencies
- **Spring Boot 3.3.4** - Application framework
- **Spring Cloud Gateway** - API Gateway functionality
- **Spring WebFlux** - Reactive web framework
- **Redis** - Rate limiting and caching
- **OpenTelemetry** - Distributed tracing
- **Prometheus** - Metrics collection

### Optional Dependencies
- **Resilience4j** - Circuit breaker and resilience
- **Jackson** - JSON processing
- **Kotlin Stdlib** - Kotlin support

## Performance Tuning

### Connection Pooling
Configure connection pool settings for optimal performance:
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### Thread Pool
Configure web server threads:
```yaml
spring:
  webflux:
    base-path: /
server:
  netty:
    connection-timeout: 10s
```

### Rate Limiting
Adjust rate limits based on load:
```yaml
redis-rate-limiter:
  replenishRate: 10     # Tokens per second
  burstCapacity: 20     # Maximum burst size
```

## Development

### Code Style
- Kotlin coding conventions
- ktlint for code formatting
- Detekt for static analysis

### IDE Setup
- IntelliJ IDEA recommended
- Kotlin plugin enabled
- Spring Boot plugin
- WebFlux support

### Testing Strategy
- Unit tests for filters and resolvers
- Integration tests for routing
- Load tests for rate limiting
- End-to-end tests for full flow

## Deployment

### Docker Deployment
```bash
# Build
docker build -f services/api-gateway/Dockerfile -t mercury-api-gateway:latest .

# Run
docker run -d -p 8080:8080 \
  -e REDIS_HOST=redis \
  --name api-gateway \
  mercury-api-gateway:latest
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: mercury-api-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: REDIS_HOST
          value: "redis-service"
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

### Production Considerations
- Use external configuration management
- Implement proper secret management for Redis
- Set up monitoring and alerting
- Configure log aggregation
- Implement backup Redis for high availability
- Use load balancer in front of gateway instances

## Recent Updates

### Latest Improvements
- Fixed compilation issues
- Added comprehensive health endpoints
- Implemented rate limiting with multiple strategies
- Added request/response logging
- Enhanced configuration management
- Improved error handling and resilience

### Version
- **Current Version**: 0.1.0
- **Last Updated**: December 2024

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/gateway-feature`)
3. Commit your changes (`git commit -m 'Add gateway feature'`)
4. Push to the branch (`git push origin feature/gateway-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## Support

For questions and support:
- Create an issue in the repository
- Contact the development team
- Check the project documentation in `/docs`
