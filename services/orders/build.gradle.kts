plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":common:common-events"))
    implementation(project(":common:common-tracing"))
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}