/**
 * Быстрый performance-smoke: мало VU, короткая длительность.
 * Запуск: k6 run -e BASE_URL=https://localhost:8443 -e EMAIL=... -e PASSWORD=... smoke.js
 * (для самоподписанного TLS: K6_SKIP_TLS_VERIFY=true)
 */
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 2,
  duration: "20s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<3000"],
  },
};

const base = __ENV.BASE_URL || "http://localhost:8080";
const email = __ENV.EMAIL || "";
const password = __ENV.PASSWORD || "";

function loginToken() {
  if (!email || !password) return "";
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
  check(r, { "universities 200": (res) => res.status === 200 });

  const token = loginToken();
  if (token) {
    const h = { headers: { Authorization: `Bearer ${token}` } };
    r = http.get(`${base}/api/v1/schedule`, h);
    check(r, { "schedule auth": (res) => res.status === 200 || res.status === 403 });
  }
  sleep(0.3);
}
