plugins {
    kotlin("jvm")
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("org.springframework.kafka:spring-kafka:3.1.4")
    implementation("org.springframework:spring-context:6.1.5")
    implementation("io.opentelemetry:opentelemetry-api:1.32.0")
    implementation("io.micrometer:micrometer-core:1.11.5")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.4")
    testImplementation("org.springframework.kafka:spring-kafka-test:3.1.4")
}
