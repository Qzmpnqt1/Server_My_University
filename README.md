# Мой Университет - Серверная часть

Серверное приложение для проекта "Мой Университет" с поддержкой чатов на Cassandra.

## Требования

* Java 17
* Docker и Docker Compose
* MySQL 8
* Cassandra 4.x

## Запуск через Docker

1. Запустите контейнеры MySQL и Cassandra:

```bash
docker-compose up -d
```

2. Дождитесь полной инициализации (примерно 1-2 минуты для Cassandra)

3. Запустите приложение:

```bash
./gradlew bootRun
```

## База данных

Проект использует две базы данных:

* **MySQL** - для основных данных приложения (пользователи, институты, группы и т.д.)
* **Cassandra** - для функциональности чатов (сообщения, участники чатов и т.д.)

## Структура проекта

* `src/main/java/org/example/model` - модели данных для MySQL
* `src/main/java/org/example/model/cassandra` - модели данных для Cassandra
* `src/main/java/org/example/repository` - репозитории JPA для MySQL
* `src/main/java/org/example/repository/cassandra` - репозитории для Cassandra
* `src/main/java/org/example/service` - сервисные интерфейсы
* `src/main/java/org/example/service/impl` - реализации сервисов
* `src/main/java/org/example/controller` - REST контроллеры

## Endpoints для чатов

* `POST /api/chats` - создать новый чат
* `GET /api/chats/user/{userId}` - получить чаты пользователя
* `POST /api/chats/{chatId}/messages` - отправить сообщение
* `GET /api/chats/{chatId}/messages` - получить сообщения чата
* `POST /api/chats/{chatId}/messages/read` - отметить сообщения как прочитанные

## Отладка Cassandra

Для подключения к Cassandra используйте:

```bash
docker exec -it my_university_cassandra cqlsh
```

Затем выполните:

```sql
USE my_university_chat;
DESCRIBE TABLES;
```

Для получения списка таблиц Cassandra. 