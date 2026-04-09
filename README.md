# My University — Backend (Server_My_University)

REST API backend for the **My University** platform — a university management system covering institutes, study directions, academic groups, schedules, grades, subject practices, user profiles, registration workflows, and real-time chat.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Relational DB | MySQL 8.0 (JPA / Hibernate) |
| NoSQL DB | Apache Cassandra 4.1 (chat messages & conversations) |
| Migrations | Flyway |
| Auth | JWT (jjwt) + Spring Security |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Gradle (Kotlin DSL) |
| Containerisation | Docker & Docker Compose |

## Architecture Overview

```
┌──────────────┐      ┌──────────────────────────────────────────┐
│  Mobile App  │─────▶│           Spring Boot API (HTTPS :8443)   │
│  (Android)   │      │                                          │
└──────────────┘      │  Controller → Service → Repository       │
                      │                                          │
                      │  ┌────────────┐    ┌─────────────────┐   │
                      │  │  MySQL 8   │    │  Cassandra 4.1  │   │
                      │  │  (main DB) │    │    (chat DB)    │   │
                      │  └────────────┘    └─────────────────┘   │
                      └──────────────────────────────────────────┘
```

**MySQL** stores all core domain entities — universities, institutes, study directions, academic groups, users, profiles, subjects, grades, schedules, classrooms, audit logs, and in-app notifications. Schema is managed by Flyway migrations (`V1`… SQL, Java `V22`, далее SQL, включая уведомления в `V24`).

**Cassandra** powers the chat subsystem with two tables optimised for time-series queries: `conversations_by_user` and `messages_by_conversation`. The keyspace is initialised automatically by the `cassandra-init` service via `init-scripts/init-cassandra.cql`.

**Security** is stateless JWT-based. Public endpoints (auth, registration, reference data, Swagger, health) are open; everything else requires a valid `Bearer` token.

## Project Structure

```
src/main/java/org/example/
├── config/             SecurityConfig, CassandraConfig, OpenApiConfig
├── controller/         REST controllers (Auth, Chat, Schedule, Grades …)
├── dto/                Request / Response DTOs
├── model/              JPA entities (MySQL)
│   └── cassandra/      Cassandra entities
├── repository/         Spring Data JPA repositories
│   └── cassandra/      Spring Data Cassandra repositories
├── security/           JWT filter & utilities
└── service/            Service interfaces
    └── impl/           Service implementations
```

## Running with Docker Compose (recommended)

This is the simplest way to start — a single command brings up MySQL, Cassandra, and the application.

```bash
# Clone the repo and navigate to the backend
cd Server_My_University

# Start everything (builds the app image on first run)
docker compose up -d --build

# Follow logs
docker compose logs -f app
```

The first start takes 2–3 minutes (Cassandra health-check + Flyway migrations). Once you see `Started ServerMyUniversityApplication`, the API is ready at **https://localhost:8443** (TLS with the bundled dev keystore; use `curl -k` for self-signed).

**TLS:** the public API is **HTTPS-only** (embedded Tomcat, default port **8443**). The repo includes `dev-keystore.p12` for local/dev use (password override: `SSL_KEYSTORE_PASSWORD`). For production, replace the keystore or terminate TLS at a reverse proxy. JDBC URLs to MySQL/Cassandra remain separate (not the mobile API surface).

To stop:

```bash
docker compose down
```

To stop **and** remove persisted data volumes:

```bash
docker compose down -v
```

## Running Locally (without Docker for the app)

Use Docker Compose only for the databases and run the Spring Boot app on the host.

### Prerequisites

- Java 17+
- Docker & Docker Compose

### Steps

1. Start MySQL and Cassandra:

```bash
docker compose up -d mysql cassandra cassandra-init
```

2. Wait for Cassandra initialisation (~1–2 minutes). Check status:

```bash
docker compose logs cassandra-init
```

3. Run the application:

```bash
./gradlew bootRun
```

The app connects to `localhost:3307` (MySQL from Docker Compose on the host; avoids clashing with a local MySQL on 3306) and `localhost:9042` (Cassandra) by default — see `application.properties`. Override with `SPRING_DATASOURCE_URL` or `MYSQL_HOST_PORT` in Compose if needed.

## API Documentation (Swagger)

Once the server is running, Swagger UI is available at:

**https://localhost:8443/swagger-ui/index.html**

The raw OpenAPI spec is at:

**https://localhost:8443/v3/api-docs**

All protected endpoints require a `Bearer <JWT>` token. Use the **Authorize** button in Swagger UI to set it after logging in.

## Default Admin Credentials

| Field | Value |
|---|---|
| Email | `admin@university.ru` |
| Password | `Admin123!` |

Obtain a JWT token via:

```bash
curl -k -X POST https://localhost:8443/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@university.ru","password":"Admin123!"}'
```

## Key API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Authenticate & receive JWT |
| `POST` | `/api/v1/auth/register` | Submit registration request |
| `GET` | `/api/v1/universities` | List universities |
| `GET` | `/api/v1/institutes` | List institutes |
| `GET` | `/api/v1/directions` | List study directions |
| `GET` | `/api/v1/groups` | List academic groups |
| `GET` | `/api/v1/schedule` | Get schedule |
| `GET` | `/api/v1/subjects` | List subjects |
| `POST` | `/api/chats` | Create a new chat |
| `GET` | `/api/chats/user/{userId}` | Get user's conversations |
| `POST` | `/api/chats/{chatId}/messages` | Send a message |
| `GET` | `/api/chats/{chatId}/messages` | Get chat messages |
| `GET` | `/actuator/health` | Health check |

## Debugging Cassandra

```bash
docker exec -it my_university_cassandra cqlsh
```

```sql
USE my_university_chat;
DESCRIBE TABLES;
SELECT * FROM conversations_by_user LIMIT 10;
```

## Environment Variables

Compose publishes MySQL on host port **3307** by default (`3307:3306`). Set `MYSQL_HOST_PORT=3306` before `docker compose up` only if nothing is listening on 3306 on your machine.

The `app` service in Docker Compose accepts the following overrides:

| Variable | Default | Description |
|---|---|---|
| `MYSQL_HOST_PORT` | `3307` | Host port mapped to MySQL 3306 |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/my_university…` | JDBC URL for MySQL |
| `SPRING_DATASOURCE_USERNAME` | `root` | MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | MySQL password |
| `SPRING_CASSANDRA_CONTACT_POINTS` | `cassandra` | Cassandra host |
| `SPRING_CASSANDRA_PORT` | `9042` | Cassandra CQL port |
| `SPRING_CASSANDRA_LOCAL_DATACENTER` | `datacenter1` | Cassandra datacenter name |
| `SPRING_CASSANDRA_KEYSPACE_NAME` | `my_university_chat` | Cassandra keyspace |
| `JWT_SECRET` | *(dev default in `application.properties`)* | Секрет подписи JWT; для staging/production задайте явно (не коммитьте) |

## CI/CD

Сборка в GitHub Actions, публикация образа в GHCR и опциональный deploy описаны в **[docs/CI_CD.md](docs/CI_CD.md)**.

Дополнительно: **[docs/DATABASE.md](docs/DATABASE.md)** (MySQL/Cassandra), **[docs/BACKUP_RESTORE.md](docs/BACKUP_RESTORE.md)**, **[docs/DIPLOM_TZ_DOCUMENTATION.md](docs/DIPLOM_TZ_DOCUMENTATION.md)**, **[docs/TZ_TRACEABILITY_MATRIX.md](docs/TZ_TRACEABILITY_MATRIX.md)**, **[docs/PROGRAMMA_I_METODIKA_ISPYTANIY.md](docs/PROGRAMMA_I_METODIKA_ISPYTANIY.md)**, **[docs/PERFORMANCE_AND_LOAD.md](docs/PERFORMANCE_AND_LOAD.md)**.
