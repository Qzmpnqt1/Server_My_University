plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

ext {
    set("springCloudVersion", "2023.0.0")
    set("testcontainersVersion", "1.19.5")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

dependencies {
    // Core Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web") // Use Spring MVC instead of WebFlux
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator") // For monitoring and metrics

    // Database
    implementation("com.mysql:mysql-connector-j:8.3.0") // Use explicit version of MySQL connector

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis") // For session storage
    implementation("redis.clients:jedis:5.1.0") // Redis client

    // Cassandra - needed for compilation but disabled in application.properties
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")

    // Kubernetes - отключены, так как не нужны на данном этапе
    // implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client")
    // implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client-config")
    // implementation("io.kubernetes:client-java:19.0.0") // Java client for Kubernetes API

    // Spring Cloud - отключен Spring Cloud Gateway
    // implementation("org.springframework.cloud:spring-cloud-starter-gateway") {
    //     exclude(group = "org.springframework.boot", module = "spring-boot-starter-webflux")
    // }
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    // WebSocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.security:spring-security-messaging")

    // JWT
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Batch processing - commented out as we've disabled batch in application.properties
    // implementation("org.springframework.boot:spring-boot-starter-batch")

    // Monitoring and tracing
    implementation("io.micrometer:micrometer-registry-prometheus") // For Prometheus metrics

    // Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0") // Change to webmvc from webflux

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:cassandra")
    testImplementation("com.redis.testcontainers:testcontainers-redis:2.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters") // For parameter name support in reflection
}