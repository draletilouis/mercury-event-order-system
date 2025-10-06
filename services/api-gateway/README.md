# API Gateway Service

The API Gateway serves as the single entry point for all client requests to the Mercury Order System. It provides enterprise-grade features including routing, rate limiting, health monitoring, and observability.

## 🚀 Features

### **Core Functionality**
- **Service Routing**: Intelligent routing to backend microservices
- **Load Balancing**: Distribution of requests across service instances
- **Request/Response Logging**: Comprehensive logging with timing metrics
- **Health Monitoring**: Kubernetes-ready health, readiness, and liveness endpoints

### **Rate Limiting**
- **Redis-based**: Scalable rate limiting with Redis backend
- **Multiple Strategies**:
  - IP-based limiting (`clientIdKeyResolver`)
  - User-Agent + IP combination (`ipUserAgentKeyResolver`)
  - User ID-based limiting (`userIdKeyResolver`)
- **Configuration**: 10 requests/second with burst capacity of 20

### **Security & Resilience**
- **CORS Support**: Pre-configured for frontend applications
- **Error Handling**: Graceful error handling and fallback mechanisms
- **Circuit Breaker**: Resilience4j integration for fault tolerance

### **Observability**
- **Prometheus Metrics**: Built-in metrics collection
- **OpenTelemetry Tracing**: Distributed tracing support
- **Health Endpoints**: Multiple health check endpoints for monitoring

## 🏗️ Architecture

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

## 📡 Endpoints

### **Health Endpoints**
- `GET /health` - General health status
- `GET /health/ready` - Readiness probe for Kubernetes
- `GET /health/live` - Liveness probe for container orchestration

### **Service Routing**
- `GET /api/orders/**` → Orders Service (Port 8082)
- `GET /api/payments/**` → Payments Service (Port 8083)
- `GET /api/inventory/**` → Inventory Service (Port 8081)

### **Monitoring**
- `GET /actuator/health` - Spring Boot actuator health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## ⚙️ Configuration

### **Application Properties**
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

### **Environment Variables**
- `REDIS_HOST` - Redis server host (default: localhost)
- `REDIS_PORT` - Redis server port (default: 6379)
- `OTLP_ENDPOINT` - OpenTelemetry collector endpoint

## 🔧 Components

### **Configuration Classes**
- `IdempotencyProperties` - Configuration properties for idempotency handling
- `RateLimitConfiguration` - Rate limiting strategies and key resolvers

### **Filters**
- `SimpleLoggingFilter` - Request/response logging with timing

### **Controllers**
- `HealthController` - Health check endpoints

## 🚀 Running the Service

### **Development Mode**
```bash
./gradlew :services:api-gateway:bootRun
```

### **Docker**
```bash
docker build -f services/api-gateway/Dockerfile -t mercury-api-gateway .
docker run -p 8080:8080 mercury-api-gateway
```

### **Docker Compose**
```bash
docker-compose up -d api-gateway
```

## 🧪 Testing

### **Health Check**
```bash
curl http://localhost:8080/health
```

### **Readiness Check**
```bash
curl http://localhost:8080/health/ready
```

### **Service Routing Test**
```bash
# This should route to the Orders service
curl http://localhost:8080/api/orders/health
```

## 📊 Monitoring

### **Metrics**
- Request count and duration
- Rate limiting metrics
- Error rates and response codes
- Service routing statistics

### **Health Checks**
- Service availability
- Redis connectivity
- Backend service health

### **Logging**
- Request/response logging
- Performance timing
- Error tracking
- Rate limiting events

## 🔒 Security Considerations

### **Rate Limiting**
- Protects against DDoS attacks
- Prevents API abuse
- Configurable per client/IP

### **CORS**
- Pre-configured for localhost:3000
- Configurable for production domains

### **Future Enhancements**
- JWT authentication
- API key management
- Request validation
- Security headers

## 🐛 Troubleshooting

### **Common Issues**

1. **Port Already in Use**
   ```bash
   # Kill process using port 8080
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F
   ```

2. **Redis Connection Issues**
   - Ensure Redis is running on the configured host/port
   - Check Redis connectivity: `telnet localhost 6379`

3. **Backend Service Unavailable**
   - Verify backend services are running
   - Check service health endpoints
   - Review routing configuration

### **Debug Mode**
```bash
./gradlew :services:api-gateway:bootRun --debug
```

## 📚 Dependencies

- **Spring Boot 3.3.4** - Application framework
- **Spring Cloud Gateway** - API Gateway functionality
- **Spring WebFlux** - Reactive web framework
- **Redis** - Rate limiting and caching
- **OpenTelemetry** - Distributed tracing
- **Prometheus** - Metrics collection

## 🔄 Recent Updates

### **Latest Improvements**
- ✅ Fixed compilation issues
- ✅ Added comprehensive health endpoints
- ✅ Implemented rate limiting with multiple strategies
- ✅ Added request/response logging
- ✅ Enhanced configuration management
- ✅ Improved error handling and resilience

### **Version**: 0.1.0
### **Last Updated**: December 2024
