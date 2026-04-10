#!/usr/bin/env bash
# Нагрузочный smoke через Docker (k6 не нужен локально).
# Пример:
#   export BASE_URL=https://localhost:8443 K6_SKIP_TLS_VERIFY=true
#   ./run-smoke-docker.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-http://host.docker.internal:8080}"

docker run --rm \
  -v "$HERE:/scripts:ro" \
  -w /scripts \
  -e BASE_URL="$BASE_URL" \
  -e EMAIL="${EMAIL:-}" \
  -e PASSWORD="${PASSWORD:-}" \
  -e K6_SKIP_TLS_VERIFY="${K6_SKIP_TLS_VERIFY:-}" \
  grafana/k6:latest run smoke.js
