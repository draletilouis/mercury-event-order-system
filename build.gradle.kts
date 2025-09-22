plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    kotlin("plugin.jpa") version "2.0.21" apply false
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("com.google.cloud.tools.jib") version "3.4.0" apply false
}

allprojects {
    group = "com.mercury.orders"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")

    // Ensure consistent toolchains for Gradle 8+ and Kotlin 2.0+
    extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java) {
        jvmToolchain(17)
    }
    extensions.configure(org.gradle.api.plugins.JavaPluginExtension::class.java) {
        toolchain {
            languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
        }
    }


    // Apply Spring Boot plugins to service modules
    if (name in listOf("api-gateway", "inventory", "orders", "payments")) {
        apply(plugin = "org.springframework.boot")
        apply(plugin = "org.jetbrains.kotlin.plugin.spring")
        apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    }

    // Import Spring Boot dependency BOM for consistent versions across all modules
    extensions.configure(io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension::class.java) {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
        }
    }

    // Testing configuration: ensure JUnit 5 and avoid Gradle 9 no-test failure
    val hasAnyTests = sequenceOf(
        file("src/test/kotlin"),
        file("src/test/java")
    ).any { dir -> dir.exists() && dir.walkTopDown().any { it.isFile && (it.name.endsWith(".kt") || it.name.endsWith(".java")) } }

    tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
        useJUnitPlatform()
        onlyIf { hasAnyTests }
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

    // Kotlin compiler options
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xjsr305=strict")
            jvmTarget = "17"
            allWarningsAsErrors = false
        }
    }
}

// Expose build info in Spring Boot apps
subprojects {
    plugins.withId("org.springframework.boot") {
        // Add Spring Boot test starter where Boot manages dependency versions
        dependencies {
            add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        }
        extensions.configure(org.springframework.boot.gradle.dsl.SpringBootExtension::class.java) {
            buildInfo()
        }
    }
}