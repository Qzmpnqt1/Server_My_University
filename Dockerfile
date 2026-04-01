# Стадия сборки: официальный образ уже содержит Gradle — не качаем gradle-8.10-bin.zip
# (внутри Docker часто таймаут к services.gradle.org).
FROM gradle:8.10-jdk17 AS builder
WORKDIR /app
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts ./
COPY --chown=gradle:gradle src src
# WORKDIR /app создаётся от root — без chown пользователь gradle не может писать /app/.gradle
USER root
RUN chown -R gradle:gradle /app
USER gradle
RUN gradle bootJar --no-daemon --no-build-cache

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
