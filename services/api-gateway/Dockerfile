FROM openjdk:17-jdk-slim

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
COPY services/api-gateway services/api-gateway

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew :services/api-gateway:build -x test

# Run the application
EXPOSE 8080

CMD ["java", "-jar", "services/api-gateway/build/libs/api-gateway-0.1.0.jar"]































