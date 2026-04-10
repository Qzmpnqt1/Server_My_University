# Производительность и нагрузочное тестирование

## Анализ инструментов (требование практики / ТЗ)

| Инструмент | Назначение | Применимость к проекту |
|------------|------------|-------------------------|
| **k6** | HTTP-сценарии, пороги p95 | Удобно для REST на `https://host:8443`, скрипты на JS |
| **Apache JMeter** | GUI + распределённая нагрузка | Тяжелее, зато привычен в вузах |
| **Gatling** | Scala DSL, отчёты | Хорош для CI, круче порог входа |
| **Apache Bench (ab)** | Быстрый sanity-check | Нет TLS-сложных сценариев без доп. флагов |

Рекомендация для дипломного стенда: **k6** или **JMeter** для сценариев «логин → типовые GET» с проверкой p95.

## Что уже есть в проекте

- Лог длительности запроса: `HttpRequestLoggingFilter` (`HTTP done ... (N ms)`).  
- Actuator **metrics** (с JWT): JVM, Tomcat.  
- Скрипты **k6** в каталоге `load/k6/`:
  - **`smoke.js`** — короткий прогон, мягкие пороги (`p(95)<3000` ms), подходит для sanity-check и лёгкого CI.
  - **`full.js`** — ступенчатая нагрузка, параметры `VUS`, `DURATION`, опционально `PRACTICE_ID`, `CHAT_PEER_USER_ID`, `STUDENT_USER_ID`.
  - **`tz-stress-profile.js`** — профиль под формулировки ТЗ: по умолчанию **1000 VU**, порог **`p(95)<200` ms**; предназначен для **выделенного стенда**, не для бесплатных runner’ов GitHub Actions.

### Как запускать локально / на стенде

```bash
docker run --rm -v "$(pwd)/load/k6:/scripts" -w /scripts \
  -e BASE_URL=https://api.example.com \
  -e EMAIL=user@example.com -e PASSWORD='***' \
  -e K6_SKIP_TLS_VERIFY=false \
  grafana/k6:latest run smoke.js
```

Для профиля ТЗ (жёсткие пороги):

```bash
docker run --rm -v "$(pwd)/load/k6:/scripts" -w /scripts \
  -e BASE_URL=... -e EMAIL=... -e PASSWORD='...' \
  -e VUS=1000 -e DURATION=2m \
  grafana/k6:latest run tz-stress-profile.js
```

Успех: процесс завершается с кодом **0** и в stdout нет провалов порогов k6. Итоговая сводка — в конце вывода `k6`.

### CI

- **Ручной smoke:** `.github/workflows/k6-smoke.yml` (`workflow_dispatch`, поле `base_url`).
- **По расписанию (опционально):** `.github/workflows/k6-nightly.yml` — требуется repository variable **`K6_BASE_URL`** или ручной запуск с `base_url`.

## Нефункциональные цели ТЗ

Цели **200 мс / p95**, **1000 одновременных запросов** проверяются скриптом **`tz-stress-profile.js`** на инфраструктуре, сопоставимой с промышленной. На слабом окружении пороги могут не выполняться — это **не дефект кода**, а ограничение стенда; в отчёте указывают фактическую конфигурацию и результат прогона.
