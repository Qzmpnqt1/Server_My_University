# Нагрузочный smoke через Docker (k6 не нужен локально).
# Пример:
#   $env:BASE_URL = "https://localhost:8443"
#   $env:K6_SKIP_TLS_VERIFY = "true"
#   .\run-smoke-docker.ps1

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$base = if ($env:BASE_URL) { $env:BASE_URL } else { "http://host.docker.internal:8080" }

docker run --rm `
  -v "${here}:/scripts:ro" `
  -w /scripts `
  -e BASE_URL="$base" `
  -e EMAIL="$($env:EMAIL)" `
  -e PASSWORD="$($env:PASSWORD)" `
  -e K6_SKIP_TLS_VERIFY="$($env:K6_SKIP_TLS_VERIFY)" `
  grafana/k6:latest run smoke.js
