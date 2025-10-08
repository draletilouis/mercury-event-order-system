# Multi-stage build for better security and smaller image size
FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy common modules
COPY common common

# Copy service specific files
COPY services/inventory services/inventory

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application with optimizations
RUN ./gradlew :services:inventory:build -x test --no-daemon --parallel

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Install necessary packages
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=UTC

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/services/inventory/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
