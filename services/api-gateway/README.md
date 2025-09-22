## API Gateway (Spring Cloud Gateway)

### Overview
Reactive API Gateway that fronts the Mercury microservices, providing routing, cross-cutting filters (correlation ID, logging, idempotency), rate limiting, CORS, and observability.

### Features
- Routing to downstream services with prefix stripping
- Global filters:
  - Correlation ID propagation (`X-Correlation-Id`)
  - Structured request/response logging
  - Idempotency for POST requests via Redis (`X-Idempotency-Key`)
  - Rate limiting using Redis with a custom key resolver
- CORS configuration
- Actuator metrics (Prometheus), tracing integration hooks

### Prerequisites
- JDK 17+
- Redis (local default at localhost:6379)
- Optional: Jaeger/OTLP collector for tracing

### Configuration
Main configuration is in `src/main/resources/application.yml`.
- Server: listens on port 8080
- Routes:
  - `/api/orders/**` → `http://localhost:8082`
  - `/api/payments/**` → `http://localhost:8083`
  - `/api/inventory/**` → `http://localhost:8081`
- Default filters:
  - `RequestRateLimiter` with `ClientIdKeyResolver`
- Global CORS for local dev (adjust as needed)
- Redis: `spring.data.redis.host`, `spring.data.redis.port`
- Idempotency props under `app.idempotency.*`

Environment variables (optional with defaults):
- `REDIS_HOST` (default: `localhost`)
- `REDIS_PORT` (default: `6379`)
- `OTLP_ENDPOINT` (default: `http://localhost:4317`)

### Run locally
- Build (skip tests):
```bash
./gradlew :services:api-gateway:build -x test
```
- Run:
```bash
./gradlew :services:api-gateway:bootRun
```
- From IDE: run `com.mercury.orders.apigateway.ApiGatewayApplication` main.

### Useful endpoints
- Health: `GET http://localhost:8080/actuator/health`
- Metrics: `GET http://localhost:8080/actuator/prometheus`

### Example requests
- Orders passthrough:
```bash
curl -H "X-Correlation-Id: demo-123" http://localhost:8080/api/orders/health
```
- Idempotent POST (replayed while TTL valid):
```bash
curl -X POST \
  -H "X-Idempotency-Key: order-123" \
  -H "Content-Type: application/json" \
  -d '{"sku":"ABC","qty":1}' \
  http://localhost:8080/api/orders
```
- Rate-limited client identification:
  - Prefer header `X-Client-Id`, else `Authorization` value, else remote IP.

### Architecture notes
- Built on WebFlux and Spring Cloud Gateway (reactive only)
- Redis is used for:
  - Request rate limiting (bucket-based via Gateway)
  - Idempotency records (stores status/headers; body replay can be added later if needed)
- Filters:
  - `CorrelationIdFilter` (highest precedence): ensures/propagates `X-Correlation-Id`
  - `LoggingFilter` (lowest precedence): logs request/response, duration, route id
  - `IdempotencyFilter` (low precedence): applies to POST requests with `X-Idempotency-Key`
  - `ClientIdKeyResolver`: resolves a stable key for rate limiting

### Extending
- Circuit breakers/retries: add Resilience4j filters per route in `application.yml`
- Security: add JWT/OIDC validation filter and protected route predicates
- Response replay for idempotency: wrap/decorate response to capture body

### Docker
When Docker is available:
```bash
docker-compose build api-gateway
docker-compose up api-gateway
```
The compose file sets `REDIS_HOST`, exposes 8080, and can bring up observability stack.

### Troubleshooting
- Windows/WSL warnings like "File not found or not readable: /etc/lsb-release" or libudev messages are harmless.
- If Gradle clean/build fails due to file locks on Windows:
  - Close running Java/Gradle/IDE build processes
  - Retry with `--no-daemon` or build inside Docker
  - Prefer running all Gradle commands from a WSL terminal at the repo root
  - Ensure Redis is reachable from WSL (default localhost:6379 in compose)


