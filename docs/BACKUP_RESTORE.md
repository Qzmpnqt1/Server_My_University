# Резервное копирование и восстановление

Документ соответствует требованию ТЗ о резервировании MySQL и сохранности данных Cassandra (чат).

## MySQL

### Через Docker (рекомендуется для compose из репозитория)

Контейнер по умолчанию: `my_university_mysql`, БД: `my_university`, пользователь `root` (см. `docker-compose.yml`).

**Создать дамп (PowerShell, из каталога `Server_My_University`):**

```powershell
.\scripts\backup-mysql.ps1
```

**Восстановить:**

```powershell
.\scripts\restore-mysql.ps1 -SqlFile .\my_university_backup_YYYYMMDD_HHMMSS.sql
```

### Вручную (mysqldump)

```bash
mysqldump -h 127.0.0.1 -P 3307 -uroot -proot --single-transaction --routines my_university > backup.sql
mysql -h 127.0.0.1 -P 3307 -uroot -proot my_university < backup.sql
```

Порт `3307` — публикация из `docker-compose.yml` на хост (внутри сети compose — `3306`).

## Apache Cassandra (история сообщений)

Ключевое пространство задаётся в `application.properties` / переменных окружения (`SPRING_CASSANDRA_KEYSPACE_NAME`, по умолчанию `my_university_chat`).

**Снимок (на узле Cassandra):**

```bash
nodetool snapshot my_university_chat
```

Снимки хранятся в данных контейнера (`cassandra_data` volume). Для переноса — скопировать соответствующий каталог снимка из `data` + метаданные.

**Экспорт отдельной таблицы (cqlsh):**

```bash
cqlsh -e "COPY my_university_chat.messages_by_conversation TO 'messages.csv';"
```

Полная процедура восстановления Cassandra зависит от топологии кластера; для учебного single-node достаточно восстановления volume или повторной инициализации `init-scripts/init-cassandra.cql` на пустом томе (история при этом теряется).

## Рекомендации

- Делать дампы MySQL перед миграциями Flyway на продакшене.
- Хранить бэкапы вне сервера приложения.
- Периодичность: по регламенту вуза; для дипломного стенда — перед демонстрацией.
