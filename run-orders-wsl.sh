#!/bin/bash

# WSL script to run Mercury Orders Service with test profile
echo "🚀 Starting Mercury Orders Service in WSL..."
echo "📝 Using test profile with H2 database"

export SPRING_PROFILES_ACTIVE=test
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=testdb
export SHOW_SQL=true

# Run the service
./gradlew :services:orders:bootRun \
    --no-daemon \
    --console=plain \
    --args="--spring.profiles.active=test --spring.datasource.url=jdbc:h2:mem:testdb --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password=password"

















