#!/bin/bash

# WSL script to run Mercury Orders Service tests
echo "🧪 Running Mercury Orders Service Tests in WSL..."
echo "📝 Using test profile with H2 database"

export SPRING_PROFILES_ACTIVE=test
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=testdb
export SHOW_SQL=true

# Run the tests
./gradlew :services:orders:test \
    --no-daemon \
    --console=plain \
    --info

echo "✅ Tests completed!"

