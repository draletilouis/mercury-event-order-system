# Payments Service

The Payments Service authorizes and reverses payments and publishes domain events using the outbox pattern.

## Overview
- Port: 8083
- Tech: Kotlin, Spring Boot, JPA, Kafka, Redis
- DB: PostgreSQL (Flyway), H2 for tests
- Outbox: `outbox_events` table with scheduled publisher

## Quick Start (WSL)
```bash
cd /mnt/c/Users/Admin/IdeaProjects/mercury-order-system

# Start infra (Kafka, Redis, Postgres recommended)
docker-compose up -d postgres redis kafka zookeeper

# Run service
./gradlew :services:payments:bootRun
```

Run with profile:
```bash
./gradlew :services:payments:bootRun -Dspring.profiles.active=dev
```

## API
- Actuator Health: `GET http://localhost:8083/actuator/health`

## Events
- Publishes: `PaymentAuthorized`, `PaymentDeclined`, `PaymentReversed`
- Topic: typically `payment-events` (see common events config)

## Configuration
- DB/Kafka/Redis configured in `src/main/resources/application.yml`
- Common env vars: `DB_*`, `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `SPRING_PROFILES_ACTIVE`

## Docker
```bash
docker build -f services/payments/Dockerfile -t mercury-payments-service .
docker run -p 8083:8083 mercury-payments-service
```

## Troubleshooting (WSL)
- udev/lsb-release warnings are harmless. Optionally install: `sudo apt-get install -y libudev-dev lsb-release`.
- If Gradle daemon crashes, rebuild with `--no-daemon`.


