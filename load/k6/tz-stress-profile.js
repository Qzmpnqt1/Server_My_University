/**
 * Профиль, соответствующий формулировкам ТЗ про нагрузку (много VU, жёсткий p95).
 * НЕ предназначен для GitHub Actions free runner — только выделенный стенд / защита.
 *
 * Пример:
 *   k6 run -e BASE_URL=https://api.example.com -e EMAIL=... -e PASSWORD=... \
 *     -e VUS=1000 -e DURATION=2m tz-stress-profile.js
 *
 * Пороги: p(95) < 200 ms по длительности HTTP-запроса (как целевой ориентир ТЗ);
 * фактическое выполнение зависит от CPU БД, сети TLS и конфигурации сервера.
 */
import http from "k6/http";
import { check, sleep } from "k6";

const base = __ENV.BASE_URL || "http://localhost:8080";
const email = __ENV.EMAIL || "";
const password = __ENV.PASSWORD || "";
const vus = Number(__ENV.VUS || 1000);
const duration = __ENV.DURATION || "2m";

export const options = {
  scenarios: {
    open: {
      executor: "constant-vus",
      vus: vus,
      duration: duration,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<200"],
  },
};

function login() {
  const res = http.post(
    `${base}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  if (res.status !== 200) return "";
  try {
    return JSON.parse(res.body).token || "";
  } catch {
    return "";
  }
}

export default function () {
  let r = http.get(`${base}/api/v1/universities`);
  check(r, { universities_ok: (x) => x.status === 200 });

  const token = login();
  if (token) {
    const h = { headers: { Authorization: `Bearer ${token}` } };
    r = http.get(`${base}/api/v1/schedule`, h);
    check(r, { schedule: (x) => x.status === 200 || x.status === 403 });
    r = http.get(`${base}/api/v1/grades/my`, h);
    check(r, { gradebook: (x) => x.status === 200 || x.status === 403 });
  }
  sleep(0.05);
}
