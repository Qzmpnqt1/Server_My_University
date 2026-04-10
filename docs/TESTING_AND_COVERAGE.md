# Тестирование и покрытие (Server_My_University)

## Виды испытаний и команды

| Вид | Gradle / инструмент | Описание |
|-----|---------------------|----------|
| Модульные + WebMvc | `./gradlew test` | JUnit 5, `src/test/java`, без Testcontainers. |
| Безопасность (негатив) | `./gradlew securityTest` | Подмножество `src/test` с `@Tag("security")`. |
| Интеграционные | `./gradlew integrationTest` | `src/integrationTest`, Spring Boot + Flyway + Testcontainers (MySQL + Cassandra). |
| Функциональные API (IT) | `./gradlew functionalApiTest` | Класс `CombinedIntegrationIT`, только методы с `@Tag("functional")` (**13** сценариев: пользовательские цепочки без публичных actuator/universities-smoke). |
| Регрессия (полный JVM) | `./gradlew regressionTest` | `test` + `securityTest` + `integrationTest` подряд. |
| JaCoCo unit | `./gradlew jacocoTestReport` | После `test`. |
| JaCoCo полный бандл | `./gradlew jacocoFullReport` | После `test`, `securityTest`, `integrationTest`. |
| Gate ключевых пакетов | `./gradlew jacocoKeyPackagesVerification` | Пороги для выбранных пакетов (см. ниже). |

## Что считается «ключевым функционалом» для JaCoCo gate

Цель — честный контроль регрессий, а не искусственные 75 % по всему репозиторию.

В `build.gradle.kts` задача **jacocoKeyPackagesVerification** проверяет **INSTRUCTION coverage** по:

- `org.example.security` — **минимум 75 %**
- `org.example.service.impl` — **минимум 55 %**
- `org.example.util` — **минимум 57 %**

Отчёты HTML/XML: `build/reports/jacoco/`, для полного бандла — `build/reports/jacoco/full/html/`.

Пакеты **controller**, **dto**, **model** в gate намеренно не включены (большой объём DTO/моделей раздувает метрики без смысла для дипломного «ключевого» критерия).

Сквозные сценарии (регистрация, оценки, практики, чат, SUPER_ADMIN, изоляция данных) дополнительно закрываются **`CombinedIntegrationIT`** и не дублируются процентом JaCoCo.

## CI

- **Backend CI** (`.github/workflows/backend-ci.yml`): `test` + `securityTest`, отдельно `integrationTest`, затем merge `*.exec` и `jacocoKeyPackagesVerifyMergedCi`.
- **Регрессия nightly** (`.github/workflows/backend-regression-nightly.yml`): полный `./gradlew regressionTest`.
- **k6** (`.github/workflows/k6-smoke.yml`, `k6-nightly.yml`): ручной / по расписанию smoke; профиль ТЗ — `load/k6/tz-stress-profile.js` на стенде. Локально без установки k6: `load/k6/run-smoke-docker.sh` / `run-smoke-docker.ps1`. Подробнее: `load/k6/README.md`.

## Параллельность

- `test` / `securityTest`: `maxParallelForks` по числу ядер (см. `build.gradle.kts`).
- `integrationTest`: один форк (`maxParallelForks = 1`), параллель JUnit отключён — стабильность Testcontainers и одного Spring-контекста.
