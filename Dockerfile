# Без "# syntax=docker/dockerfile:1": иначе BuildKit тянет образ docker/dockerfile с Docker Hub (TLS timeout у части сетей).
#
# Если Gradle не может скачать зависимости: "Could not GET ... repo.maven.apache.org" / "Temporary failure in name resolution"
# — это DNS внутри Docker build, не ошибка репозитория. Что сделать:
#   Docker Desktop → Settings → Docker Engine → в JSON добавьте:  "dns": ["8.8.8.8", "1.1.1.1"]
#   либо отключите VPN/смените сеть. Сборка: DOCKER_BUILDKIT=1 docker compose build app
#
# Стадия сборки: образы с docker.io (gradle, eclipse-temurin). При TLS timeout к Hub см. Dockerfile.mcr + docker-compose.app-mcr.yml
# (сборка JAR локально: gradlew bootJar, рантайм с mcr.microsoft.com/openjdk).
# Официальный образ уже содержит Gradle — не качаем gradle-8.10-bin.zip (внутри Docker часто таймаут к services.gradle.org).

FROM gradle:8.10-jdk17 AS builder
WORKDIR /app
ENV GRADLE_OPTS="-Dorg.gradle.internal.http.connectionTimeout=180000 -Dorg.gradle.internal.http.socketTimeout=180000"
# Часть сетей/прокси рвёт TLS 1.3 handshake с Maven; только builder — на рантайм JAR не влияет.
ENV JAVA_TOOL_OPTIONS="-Djdk.tls.client.protocols=TLSv1.2 -Dhttps.protocols=TLSv1.2"
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts ./
COPY --chown=gradle:gradle src src
# WORKDIR /app создаётся от root — без chown пользователь gradle не может писать /app/.gradle
USER root
RUN chown -R gradle:gradle /app
USER gradle
# Без RUN --mount=type=cache для ~/.gradle: на Docker Desktop (Win) mount на caches ломает создание generated-gradle-jars.
RUN gradle bootJar --no-daemon --no-build-cache

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
