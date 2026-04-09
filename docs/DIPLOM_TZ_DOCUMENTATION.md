# Документация под диплом и практику: соответствие ТЗ

Документ опирается на **фактическую** реализацию в репозиториях `App_My_University` и `Server_My_University`.

## 1. Архитектура

- **Клиент:** Android, Kotlin, Jetpack Compose, REST (Retrofit), HTTPS-only к API.  
- **Сервер:** Spring Boot 3, Java 17, REST, JWT, Spring Security RBAC.  
- **MySQL 8:** транзакционные данные (структура вуза, пользователи, расписание, оценки, заявки, аудит, **in-app уведомления**).  
- **Cassandra 4.1:** история чатов (`init-scripts/init-cassandra.cql`).  
- **Миграции:** Flyway `src/main/resources/db/migration` + Java-миграция `db/migration/V22__*.java`.  
- **Контейнеры:** `docker-compose.yml`, `Dockerfile`.

Подробнее: `README.md` в корне backend.

## 2. Роли и экраны (обзор)

| Роль (ТЗ) | Реализация | Главный вход |
|-----------|------------|--------------|
| Гость | Регистрация, статус заявки, справочники (открытые GET) | `OnboardingWelcomeScreen`, `LoginScreen`, `RegistrationScreen` |
| Студент | Расписание группы, зачётка, практики, успеваемость, чат, профиль | `HomeScreen`, `GradeBookScreen`, … |
| Преподаватель | Расписание, оценки, практики, журнал/каталог, статистика, чат | `TeacherHomeScreen`, `TeacherGradesScreen`, … |
| Администратор | Заявки, структура, расписание, пользователи, аудит, статистика | `AdminHomeScreen`, вложенные admin-экраны |
| Расширение | `SUPER_ADMIN` — без удаления; те же ветки навигации, что у `ADMIN`, с серверным scope по вузам | `UserType.SUPER_ADMIN` |

Полный граф маршрутов: `App_My_University/.../ui/navigation/AppNavigation.kt`.

## 3. Навигация

- Табы и вложенные переходы: `RoleNavigation.kt`, `BottomNavTabMapping.kt`.  
- Уведомления: маршрут `notifications`, вход с **Профиля**.

## 4. База данных (MySQL)

Цепочка **вуз → институт → направление → группа → дисциплина (учебный план)** отражена сущностями и миграциями (`University`, `Institute`, `StudyDirection`, `AcademicGroup`, `SubjectInDirection`, `Subject`).  
Оценки: `grades`, практики: `subject_practices`, `practice_grades`.  
Уведомления: таблица `in_app_notifications` (миграция `V24__in_app_notifications.sql`).

## 5. Ключевые требования ТЗ (статус)

| Требование | Где в коде |
|------------|------------|
| Регистрация → модерация → активация учётки | `RegistrationServiceImpl`, `RegistrationRequestController`, Android `RegistrationRequestsScreen` |
| JWT, защита API | `SecurityConfig`, `JwtAuthFilter` |
| Расписание, конфликты преподаватель/группа/аудитория | `ScheduleServiceImpl.checkConflicts`, `ScheduleRepository.find*Conflicts` |
| Оценки 2–5 и зачёт/незачёт, практики отдельно, **итог не из практик** | `GradeServiceImpl`, `PracticeGradeServiceImpl`, `StatisticsFinalAssessmentUtil`; итог вводится вручную |
| Согласованность курса группы и дисциплины | `CourseConsistency`, фильтры в `GradeServiceImpl`, `TeacherGradingCatalogServiceImpl`, статистика |
| Чаты в Cassandra | `ChatServiceImpl`, Cassandra-репозитории |
| HTTPS | `application.properties`, `server.ssl.*`; на клиенте запрет cleartext |
| BCrypt | `SecurityConfig` → `BCryptPasswordEncoder` |
| Логирование запросов | `HttpRequestLoggingFilter` (время ответа в логе) |
| Мониторинг | Actuator: `health` (публично), `info`, `metrics` (с JWT) |
| Уведомления о событиях | `PersistentNotificationService` + API `/api/v1/notifications/*` + экран в Android |

## 6. Нефункциональные требования (честная оценка)

- **p95 ≤ 200 мс, 1000 RPS, 99.9%** — в репозитории нет продакшен-бенчмарка; замер частично возможен по логам `HttpRequestLoggingFilter` и метрикам JVM (`/actuator/metrics`). Для отчёта нужен отдельный стенд (k6/JMeter) — см. `docs/PERFORMANCE_AND_LOAD.md`.  
- **Покрытие ≥ 75%** — включён JaCoCo (`./gradlew test jacocoTestReport`); целевой процент достигается наращиванием тестов, отчёт: `build/reports/jacoco/test/html/index.html`.

## 7. Резервное копирование

`docs/BACKUP_RESTORE.md`, скрипты `scripts/backup-mysql.ps1`, `scripts/restore-mysql.ps1`.

## 8. Тестирование и приёмка

- Программа и методика испытаний: `docs/PROGRAMMA_I_METODIKA_ISPYTANIY.md`.  
- Матрица трассировки: `docs/TZ_TRACEABILITY_MATRIX.md`.

## 9. TLS 1.3

В `application.properties` задано `server.ssl.enabled-protocols=TLSv1.3`. Если понадобится совместимость со старыми клиентами TLS 1.2, временно задайте `TLSv1.2,TLSv1.3` (компромисс для стенда).
