/**
 * Полноценный нагрузочный профиль (nightly / защита): задайте VUS/DURATION через env.
 * Пример:
 *   k6 run -e BASE_URL=https://api.example.com -e EMAIL=teacher@... -e PASSWORD=... \
 *     -e VUS=50 -e DURATION=5m full.js
 *
 * Сценарии ТЗ: login, расписание, зачётка, сохранение оценки, сообщения (при заданных id).
 */
import http from "k6/http";
import { check, sleep } from "k6";

const base = __ENV.BASE_URL || "http://localhost:8080";
const email = __ENV.EMAIL || "";
const password = __ENV.PASSWORD || "";
const vus = Number(__ENV.VUS || 20);
const duration = __ENV.DURATION || "2m";

export const options = {
  stages: [
    { duration: "30s", target: vus },
    { duration: duration, target: vus },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.1"],
    http_req_duration: ["p(95)<5000"],
  },
};

function login() {
  const res = http.post(
    `${base}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(res, { login: (r) => r.status === 200 });
  if (res.status !== 200) return "";
  try {
    return JSON.parse(res.body).token || "";
  } catch {
    return "";
  }
}

export default function () {
  const token = login();
  if (!token) {
    sleep(1);
    return;
  }
  const h = { headers: { Authorization: `Bearer ${token}` } };

  let r = http.get(`${base}/api/v1/schedule`, h);
  check(r, { schedule: (x) => x.status === 200 || x.status === 403 });

  r = http.get(`${base}/api/v1/grades/my`, h);
  check(r, { gradebook: (x) => x.status === 200 || x.status === 403 });

  const practiceId = __ENV.PRACTICE_ID;
  if (practiceId) {
    r = http.post(
      `${base}/api/v1/practice-grades`,
      JSON.stringify({
        practiceId: Number(practiceId),
        studentUserId: Number(__ENV.STUDENT_USER_ID || 0),
        gradeValue: 4,
      }),
      { headers: { "Content-Type": "application/json", ...h.headers } }
    );
    check(r, { practice_grade: (x) => x.status === 200 || x.status === 201 || x.status === 400 });
  }

  const peerId = __ENV.CHAT_PEER_USER_ID;
  if (peerId) {
    r = http.post(
      `${base}/api/v1/chats/messages`,
      JSON.stringify({ recipientId: Number(peerId), text: `k6 ${Date.now()}` }),
      { headers: { "Content-Type": "application/json", ...h.headers } }
    );
    check(r, { chat_send: (x) => x.status === 200 || x.status === 201 });
  }

  sleep(0.5);
}
