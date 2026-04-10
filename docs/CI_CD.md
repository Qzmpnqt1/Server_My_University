# CI/CD — Server_My_University (backend)

## Структура Git и ограничение

- Каталог **`MyUniversity`** (родитель `App_My_University` + `Server_My_University`) **не является** единым git-корнем.
- У **backend** свой репозиторий: корень = `Server_My_University` (здесь лежит `.git`).
- У **Android** — отдельный репозиторий в `App_My_University`.

**Следствие:** workflow GitHub Actions для backend находятся в **`Server_My_University/.github/workflows/`**. Пайплайны Android настраиваются в репозитории приложения; см. `App_My_University/docs/CI_CD.md`.

Чтобы получить один общий CI на push в обе части, объедините проекты в **один репозиторий (монорепо)** и перенесите workflows в его корень с path filters.

---

## Реализованные pipelines

| Workflow            | Файл                     | Назначение                          |
|---------------------|--------------------------|-------------------------------------|
| Backend CI          | `.github/workflows/backend-ci.yml`  | Сборка, тесты, `bootJar`, артефакты |
| Backend regression (nightly) | `.github/workflows/backend-regression-nightly.yml` | `./gradlew regressionTest`, `jacocoFullReport` |
| k6 smoke (manual)   | `.github/workflows/k6-smoke.yml`    | Docker k6 по `load/k6/smoke.js` |
| k6 scheduled        | `.github/workflows/k6-nightly.yml`  | Опционально: variable `K6_BASE_URL` или ручной ввод URL |
| Backend CD          | `.github/workflows/backend-cd.yml`  | Сборка Docker, push в **GHCR**, опциональный SSH deploy |

Подробнее о тестах и JaCoCo: [TESTING_AND_COVERAGE.md](TESTING_AND_COVERAGE.md).

---

## Триггеры

### Backend CI

- **push** — любая ветка  
- **pull_request** — любой PR  

### Backend CD

- **push** в ветки `main` или `master` — образ с тегами `latest`, `sha-*`  
- **push** тегов `v*` (например `v1.2.0`) — semver-теги для образа  
- **workflow_dispatch** — ручной запуск; опция **run_deploy** запускает job deploy (нужны secrets)

---

## Секреты и переменные

### Обязательные для CD (push образа)

- **`GITHUB_TOKEN`** — выдаётся автоматически; для `packages: write` уже задано в workflow через `permissions`.

### Опционально: staging / production

- **`JWT_SECRET`** — задайте в среде выполнения контейнера (или в `.env` на сервере, **не** в git). В `application.properties` используется `jwt.secret=${JWT_SECRET:…}` с dev-дефолтом для локальной разработки.

### Опционально: SSH deploy (job **deploy**)

| Secret               | Назначение                                      |
|----------------------|-------------------------------------------------|
| `DEPLOY_SSH_HOST`    | Хост сервера                                    |
| `DEPLOY_SSH_USER`    | Пользователь SSH                                |
| `DEPLOY_SSH_KEY`     | Приватный ключ SSH (полное содержимое)          |
| `DEPLOY_REMOTE_PATH` | Каталог на сервере с `docker compose` и `.env` |

На сервере ожидается команда `docker compose pull app && docker compose up -d app` из `DEPLOY_REMOTE_PATH` (как в локальном `docker-compose.yml`).

---

## Артефакты CI

- **`backend-test-reports`** — `build/reports/tests/`, `build/test-results/test/`  
- **`backend-boot-jar`** — `build/libs/*-SNAPSHOT.jar`

## Образ Docker (GHCR)

Имя образа: **`ghcr.io/<owner>/<repo>`** (в нижнем регистре), где `<repo>` — имя GitHub-репозитория backend.

Пример pull:

```bash
docker pull ghcr.io/OWNER/server_my_university:latest
```

Подставьте своего владельца и точное имя репозитория.

---

## Локальные команды (как в CI)

Из корня **этого** репозитория, **JDK 17** (`JAVA_HOME`):

```bash
./gradlew test --no-daemon
./gradlew bootJar --no-daemon
```

Локальный JDK без фиксации в репозитории: задайте `org.gradle.java.home` в **`~/.gradle/gradle.properties`** (см. `gradle.properties` в корне проекта).

---

## Исправления, сделанные ради CI/CD

- Удалён **machine-specific** `org.gradle.java.home` из versioned `gradle.properties`.
- **`jwt.secret`** вынесен в **`JWT_SECRET`** с dev-дефолтом в `application.properties`.
- Исправлены падающие unit-тесты (`GradeServiceTest`, `StatisticsServiceTest`).

---

## Связанный репозиторий Android

Сборка приложения, release APK/AAB, секреты подписи — в **`App_My_University/docs/CI_CD.md`**.
