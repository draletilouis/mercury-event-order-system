plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Core Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    
    // Kafka for event streaming
    implementation("org.springframework.kafka:spring-kafka")
    
    // JSON serialization for Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Common modules
    implementation(project(":common:common-events"))
    implementation(project(":common:common-tracing"))
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    runtimeOnly("com.h2database:h2") // for testing and development
}