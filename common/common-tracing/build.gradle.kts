plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-api:1.32.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.32.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:1.32.0-alpha")
    implementation("io.micrometer:micrometer-core:1.11.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.5")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.3.4")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.3.4")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.4")
}
