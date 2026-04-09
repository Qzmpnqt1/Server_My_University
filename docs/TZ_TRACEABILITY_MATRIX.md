# Матрица соответствия функционала техническому заданию

| Требование ТЗ | Реализация (ориентиры в коде) | Примечание |
|---------------|-------------------------------|------------|
| Клиент-сервер, Android 9+ | `App_My_University`, minSdk 29 | |
| Kotlin + Compose | `app/build.gradle.kts` | |
| Spring Boot 3 + Java 17 | `Server_My_University/build.gradle.kts` | |
| REST API | `org.example.controller.*` | |
| MySQL | Flyway + JPA entities | |
| Cassandra — история чатов | `ChatServiceImpl`, Cassandra config | |
| HTTPS | `application.properties`, Android cleartext off | |
| BCrypt | `SecurityConfig` | |
| JWT + RBAC | `JwtAuthFilter`, `@PreAuthorize` где применено, `UniversityScopeService` | |
| Гость: заявка | `RegistrationScreen`, `AuthController` / registration | |
| Админ: заявки | `RegistrationRequestsScreen`, `RegistrationRequestController` | |
| Структура вуза | CRUD вуз/институт/направление/группа/предмет + учебный план | |
| Расписание + конфликты | `ScheduleServiceImpl.checkConflicts` | |
| Оценки, практики, зачётка | `Grade*`, `Practice*`, Android `GradeBookScreen` | |
| Итог не из практик | Нет автосвязи final←среднее практик в сервисах сохранения оценки | |
| Согласованность курса | `CourseConsistency` | |
| Статистика | `StatisticsServiceImpl`, экраны статистики | |
| Уведомления | `PersistentNotificationService`, `InAppNotificationController`, Android уведомления | Отклонение заявки без УЗ — только лог |
| Профиль | `ProfileController`, `ProfileScreen` | |
| Логирование | `HttpRequestLoggingFilter`, бизнес-лог `@Slf4j` | |
| Мониторинг | Actuator health / metrics | |
| Резервирование | `docs/BACKUP_RESTORE.md`, `scripts/*.ps1` | |
| SUPER_ADMIN | `UserType`, scope на сервере | Сохранено как расширение |

Ячейки «Примечание» дополняются по результатам испытаний.
