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
COPY services/inventory services/inventory

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew :services/inventory:build -x test

# Run the application
EXPOSE 8081

CMD ["java", "-jar", "services/inventory/build/libs/inventory-0.1.0.jar"]

































