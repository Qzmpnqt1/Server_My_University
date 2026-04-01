# Когда docker.io даёт TLS timeout: соберите JAR на хосте (JDK 17), затем только упаковка в образ.
#   Windows:  gradlew.bat bootJar
#   Сборка:   docker compose -f docker-compose.yml -f docker-compose.app-mcr.yml build app
# Рантайм: Microsoft Container Registry (другой хост, не registry-1.docker.io).
FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu
WORKDIR /app
# Только bootJar (не *-plain.jar): имя заканчивается на -SNAPSHOT.jar без суффикса -plain
COPY build/libs/Server_My_University-*-SNAPSHOT.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
