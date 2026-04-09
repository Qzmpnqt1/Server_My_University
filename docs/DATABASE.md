# База данных

## MySQL 8

- Схема создаётся миграциями Flyway: `src/main/resources/db/migration/*.sql` и Java-класс `src/main/java/db/migration/V22__TeacherSubjectsSubjectInDirection.java`.
- JPA-сущности: `src/main/java/org/example/model/*.java`.
- Основные таблицы: университеты, институты, направления, группы, пользователи, профили, дисциплины, учебный план (`subjects_in_directions`), расписание, оценки, практики, заявки на регистрацию, аудит, **in_app_notifications**.

## Cassandra

- Ключевое пространство и таблицы для чата: `init-scripts/init-cassandra.cql`.
- Репозитории: `org.example.repository.cassandra`.

## ER-диаграмма

В репозитории нет отдельного `.drawio`; связи восстанавливаются по сущностям JPA и миграциям. Для отчёта можно экспортировать схему из MySQL Workbench или сгенерировать диаграмму по аннотациям.
