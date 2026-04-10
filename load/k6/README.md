# Нагрузочное тестирование (k6)

Скрипты в этой папке — **реальные** сценарии Grafana k6. Запуск возможен локально (бинарь k6) или через Docker (как в GitHub Actions).

## Файлы

| Файл | Назначение | Пороги (ориентир) |
|------|------------|-------------------|
| `smoke.js` | Быстрая проверка доступности API + опционально login/schedule | 2 VU, 20 s, p95 &lt; 3 s, ошибки &lt; 5% |
| `full.js` | Расширенный профиль: login, расписание, зачётка, опционально практики/чат (env) | ramp-up + удержание VUS/DURATION из env, p95 &lt; 5 s |
| `tz-stress-profile.js` | Профиль под **жёсткие** формулировки ТЗ (много VU, p95 &lt; 200 ms) | **Только выделенный стенд**, не PR/free runner |

## Переменные окружения

- `BASE_URL` — корень API (например `https://host:8443`, с завершающим слэшем не обязательно).
- `EMAIL` / `PASSWORD` — для шагов с авторизацией (если пусто, smoke проверит только публичные маршруты).
- `K6_SKIP_TLS_VERIFY=true` — для самоподписанного TLS (локально/стенд).
- `full.js`: `VUS`, `DURATION`; опционально `PRACTICE_ID`, `STUDENT_USER_ID`, `CHAT_PEER_USER_ID`.
- `tz-stress-profile.js`: `VUS` (по умолчанию 1000), `DURATION`.

## Примеры запуска

Из каталога `Server_My_University/load/k6`:

```bash
# Локально (установлен k6)
K6_SKIP_TLS_VERIFY=true k6 run -e BASE_URL=https://localhost:8443 smoke.js

# Через Docker — готовые обёртки (передают BASE_URL/EMAIL/PASSWORD/K6_SKIP_TLS_VERIFY)
./run-smoke-docker.sh
# Windows PowerShell:
#   .\run-smoke-docker.ps1

# Вручную (эквивалент скриптам)
docker run --rm -v "$PWD:/scripts:ro" -w /scripts \
  -e BASE_URL=https://localhost:8443 -e K6_SKIP_TLS_VERIFY=true \
  grafana/k6:latest run smoke.js
```

Полный профиль и stress-профиль запускайте только при **поднятом** бэкенде и осознанном выборе нагрузки.

## Честность относительно ТЗ

Целевые **p95 ≤ 200 ms** и **сотни/тысячи одновременных пользователей** проверяются скриптом `tz-stress-profile.js` на мощности стенда. Успех на CI без реального API или на слабом окружении **не гарантируется** и не требуется: в workflow k6 указаны условия (ручной запуск / секрет `K6_BASE_URL`).
