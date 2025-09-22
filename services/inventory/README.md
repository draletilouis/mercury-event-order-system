# Inventory Service

Manages stock items and reservations; participates in the order saga by reserving or releasing inventory and publishing events via outbox.

## Overview
- Port: 8081
- Tech: Kotlin, Spring Boot, JPA, Kafka
- DB: PostgreSQL (Flyway)

## Quick Start (WSL)
```bash
cd /mnt/c/Users/Admin/IdeaProjects/mercury-order-system
docker-compose up -d postgres kafka zookeeper
./gradlew :services:inventory:bootRun
```

Run with profile:
```bash
./gradlew :services:inventory:bootRun -Dspring.profiles.active=dev
```

## API
- Health: `GET http://localhost:8081/actuator/health`

## Events
- Publishes: `InventoryReserved`, `InventoryInsufficient`, `InventoryReleased`
- Consumes: payment/order events (see handlers)

## Docker
```bash
docker build -f services/inventory/Dockerfile -t mercury-inventory-service .
docker run -p 8081:8081 mercury-inventory-service
```

## Troubleshooting
- Ensure DB migrations applied; check service logs for Flyway.
- Kafka not ready: wait for compose to stabilize, then rerun `bootRun`.


