import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import kotlin.math.max

plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    maven { url = uri("https://repo1.maven.org/maven2/") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

sourceSets {
    create("integrationTest") {
        java.setSrcDirs(listOf("src/integrationTest/java"))
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")

    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:cassandra")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.SHORT
    }
    configure<JacocoTaskExtension> {
        isEnabled = true
        destinationFile = layout.buildDirectory.file("jacoco/${this@configureEach.name}.exec").get().asFile
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(sourceSets.main.get().output.classesDirs).asFileTree.matching {
            exclude("**/dto/**", "**/model/**", "**/config/OpenApiConfig.class")
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(layout.buildDirectory.file("jacoco/test.exec").get().asFile)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoReport>("jacocoFullReport") {
    group = "verification"
    description = "JaCoCo по unit/WebMvc + securityTest + integrationTest (после ./gradlew test securityTest integrationTest)"
    dependsOn(tasks.test, tasks.named("securityTest"), tasks.named("integrationTest"))
    classDirectories.setFrom(
        files(sourceSets.main.get().output.classesDirs).asFileTree.matching {
            exclude("**/dto/**", "**/model/**", "**/config/OpenApiConfig.class")
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("jacoco/test.exec"),
        layout.buildDirectory.file("jacoco/securityTest.exec"),
        layout.buildDirectory.file("jacoco/integrationTest.exec")
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/full/html"))
    }
}

val cores = Runtime.getRuntime().availableProcessors()
val unitForks = max(1, cores / 2)

tasks.test {
    maxParallelForks = unitForks
    // Параллелизм — между форками Gradle. JUnit concurrent внутри форка ломает Mockito @MockBean в WebMvcTest.
    useJUnitPlatform {
        excludeTags("security")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("securityTest") {
    description = "Негативные сценарии безопасности (@Tag(\"security\"))"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    maxParallelForks = unitForks
    useJUnitPlatform {
        includeTags("security")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Интеграционные тесты: Spring Boot + Flyway + Testcontainers (MySQL + Cassandra), один форк"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    maxParallelForks = 1
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("functionalApiTest") {
    description = "Функциональные API-сценарии из integrationTest (@Tag(\"functional\"))"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    maxParallelForks = 1
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    useJUnitPlatform {
        includeTags("functional")
    }
    shouldRunAfter(tasks.test)
}

tasks.register("regressionTest") {
    group = "verification"
    description = "Регрессия: unit/WebMvc + security + integration"
    dependsOn(tasks.test, tasks.named("securityTest"), tasks.named("integrationTest"))
}

/**
 * Ключевой функционал (без DTO/model): security + доменные impl + утилиты статистики/сортировки.
 * Gate раздельный: security ≥75%; service.impl и util — по фактически достигнутому уровню (анти-регрессия).
 */
tasks.register<JacocoCoverageVerification>("jacocoKeyPackagesVerification") {
    group = "verification"
    description =
        "Gate: security ≥75%, service.impl ≥55%, org.example.util ≥57% (INSTRUCTION); exec: test+securityTest+integrationTest"
    dependsOn(tasks.test, tasks.named("securityTest"), tasks.named("integrationTest"))
    classDirectories.setFrom(
        files(sourceSets.main.get().output.classesDirs).asFileTree.matching {
            include("org/example/service/impl/**/*.class")
            include("org/example/security/**/*.class")
            include("org/example/util/**/*.class")
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("jacoco/test.exec"),
        layout.buildDirectory.file("jacoco/securityTest.exec"),
        layout.buildDirectory.file("jacoco/integrationTest.exec")
    )
    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("org.example.security")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("org.example.service.impl")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.55".toBigDecimal()
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("org.example.util")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.57".toBigDecimal()
            }
        }
    }
}

/**
 * CI: без повторного прогона тестов — положите test.exec, securityTest.exec и integrationTest.exec в build/jacoco/
 * (артефакты jobs unit-and-security и integration-functional).
 */
tasks.register<JacocoCoverageVerification>("jacocoKeyPackagesVerifyMergedCi") {
    group = "verification"
    description = "JaCoCo gate (как jacocoKeyPackagesVerification) по артефактам build/jacoco/*.exec"
    classDirectories.setFrom(
        files(sourceSets.main.get().output.classesDirs).asFileTree.matching {
            include("org/example/service/impl/**/*.class")
            include("org/example/security/**/*.class")
            include("org/example/util/**/*.class")
        }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        layout.buildDirectory.file("jacoco/test.exec"),
        layout.buildDirectory.file("jacoco/securityTest.exec"),
        layout.buildDirectory.file("jacoco/integrationTest.exec")
    )
    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("org.example.security")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("org.example.service.impl")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.55".toBigDecimal()
            }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("org.example.util")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.57".toBigDecimal()
            }
        }
    }
}

tasks.check {
    // Быстрый локальный check: unit/WebMvc + негативная JWT-безопасность. IT — в CI или ./gradlew regressionTest
    dependsOn(tasks.named("securityTest"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val localProps = layout.projectDirectory.file("application-local.properties")
    if (localProps.asFile.exists()) {
        systemProperty("spring.profiles.active", "local")
    }
}
