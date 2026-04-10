package org.example.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.dto.response.AuthResponse;
import org.example.dto.response.MessageResponse;
import org.example.dto.response.UserProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Один класс = один кэшированный Spring-контекст и один пул JDBC на весь прогон,
 * без закрытия DataSource между классами (устраняет «мертвые» коннекты к MySQL в Testcontainers).
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombinedIntegrationIT extends AbstractIntegrationTest {

    private static String pendingStudentEmail;
    private static String pendingStudentPassword;
    private static String studentAccessToken;
    private static long registeredStudentUserId;

    @Test
    @Order(1)
    @DisplayName("GET /api/v1/universities без токена — 200")
    void universitiesPublic() {
        ResponseEntity<String> r = restTemplate.getForEntity("/api/v1/universities", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("GET /actuator/health без токена — 200")
    void healthPermitted() {
        ResponseEntity<String> r = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(3)
    @DisplayName("GET /actuator/metrics без токена — 401/403")
    void metricsDeniedForAnonymous() {
        ResponseEntity<Void> r = restTemplate.exchange(
                "/actuator/metrics",
                HttpMethod.GET,
                null,
                Void.class);
        assertThat(r.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @Tag("functional")
    @Order(4)
    @DisplayName("Гость подаёт заявку, админ одобряет, студент логинится")
    void guestRegisters_adminApproves_studentLogsIn() throws Exception {
        pendingStudentEmail = "itest-" + UUID.randomUUID() + "@student.local";
        pendingStudentPassword = "Secret12";

        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "Интеграция",
                  "lastName": "Тест",
                  "userType": "STUDENT",
                  "universityId": 1,
                  "groupId": 1
                }
                """.formatted(pendingStudentEmail, pendingStudentPassword);

        ResponseEntity<Void> reg = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(registerBody, jsonHeaders()),
                Void.class);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        AuthResponse adminAuth = login("admin@university.ru", "Admin123!");
        HttpHeaders adminHeaders = jsonHeadersBearer(adminAuth.getToken());

        ResponseEntity<String> list = restTemplate.exchange(
                "/api/v1/registration-requests?status=PENDING",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode arr = objectMapper.readTree(list.getBody());
        long requestId = -1;
        for (JsonNode n : arr) {
            if (pendingStudentEmail.equals(n.path("email").asText())) {
                requestId = n.path("id").asLong();
                break;
            }
        }
        assertThat(requestId).isPositive();

        ResponseEntity<Void> approve = restTemplate.exchange(
                "/api/v1/registration-requests/" + requestId + "/approve",
                HttpMethod.PUT,
                new HttpEntity<>(adminHeaders),
                Void.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);

        AuthResponse studentAuth = login(pendingStudentEmail, pendingStudentPassword);
        assertThat(studentAuth.getToken()).isNotBlank();
        studentAccessToken = studentAuth.getToken();
        assertThat(studentAuth.getEmail()).isEqualTo(pendingStudentEmail);
        assertThat(studentAuth.getUserType().name()).isEqualTo("STUDENT");
    }

    @Test
    @Tag("functional")
    @Order(5)
    @DisplayName("Студент читает свой профиль GET /api/v1/profile/me")
    void studentGetsOwnProfile() throws Exception {
        ResponseEntity<String> r = restTemplate.exchange(
                "/api/v1/profile/me",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotBlank();
        assertThat(r.getBody()).contains(pendingStudentEmail);
        UserProfileResponse me = objectMapper.readValue(r.getBody(), UserProfileResponse.class);
        registeredStudentUserId = me.getId();
        assertThat(registeredStudentUserId).isPositive();
    }

    @Test
    @Tag("functional")
    @Order(6)
    @DisplayName("Студент получает расписание группы GET /api/v1/schedule/group/1")
    void studentGetsGroupSchedule() {
        ResponseEntity<String> r = restTemplate.exchange(
                "/api/v1/schedule/group/1",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Tag("functional")
    @Order(7)
    @DisplayName("Студент получает зачётку GET /api/v1/grades/my")
    void studentGetsGradebook() {
        ResponseEntity<String> r = restTemplate.exchange(
                "/api/v1/grades/my",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Tag("functional")
    @Order(8)
    @DisplayName("Студент получает сводку успеваемости GET /api/v1/statistics/me/student")
    void studentGetsStatisticsSummary() {
        ResponseEntity<String> r = restTemplate.exchange(
                "/api/v1/statistics/me/student",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isNotBlank();
    }

    @Test
    @Tag("functional")
    @Order(9)
    @DisplayName("Студент читает уведомления и unread-count")
    void studentNotificationsFlow() {
        ResponseEntity<String> list = restTemplate.exchange(
                "/api/v1/notifications/my",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> cnt = restTemplate.exchange(
                "/api/v1/notifications/unread-count",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(cnt.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cnt.getBody()).contains("unreadCount");
    }

    @Test
    @Tag("functional")
    @Order(10)
    @DisplayName("Преподаватель выставляет итоговую оценку; студент видит её в зачётке")
    void teacherSetsFinalGrade_studentSeesInGradebook() throws Exception {
        AuthResponse teacherAuth = login("teacher.itest@moyvuz.local", "Admin123!");
        HttpHeaders teacherH = jsonHeadersBearer(teacherAuth.getToken());

        String gradeBody = """
                {
                  "studentId": %d,
                  "subjectDirectionId": 1,
                  "grade": 5,
                  "groupId": 1
                }
                """.formatted(registeredStudentUserId);

        ResponseEntity<String> created = restTemplate.exchange(
                "/api/v1/grades",
                HttpMethod.POST,
                new HttpEntity<>(gradeBody, teacherH),
                String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> mine = restTemplate.exchange(
                "/api/v1/grades/my",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody()).contains("\"grade\":5");
        assertThat(mine.getBody()).contains("Базы данных");
    }

    @Test
    @Tag("functional")
    @Order(11)
    @DisplayName("Преподаватель выставляет оценку за практику; студент видит практику")
    void teacherSetsPracticeGrade_studentSeesIt() throws Exception {
        ResponseEntity<String> practices = restTemplate.exchange(
                "/api/v1/subject-practices?subjectDirectionId=1",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(practices.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode parr = objectMapper.readTree(practices.getBody());
        long practiceId = -1;
        for (JsonNode n : parr) {
            if ("ITest практика".equals(n.path("practiceTitle").asText())
                    || n.path("practiceTitle").asText().contains("ITest")) {
                practiceId = n.path("id").asLong();
                break;
            }
        }
        assertThat(practiceId).isPositive();

        AuthResponse teacherAuth = login("teacher.itest@moyvuz.local", "Admin123!");
        HttpHeaders teacherH = jsonHeadersBearer(teacherAuth.getToken());
        String body = """
                {
                  "studentId": %d,
                  "practiceId": %d,
                  "grade": 4,
                  "groupId": 1
                }
                """.formatted(registeredStudentUserId, practiceId);

        ResponseEntity<String> pg = restTemplate.exchange(
                "/api/v1/practice-grades",
                HttpMethod.POST,
                new HttpEntity<>(body, teacherH),
                String.class);
        assertThat(pg.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> slots = restTemplate.exchange(
                "/api/v1/practice-grades/my/subject/1/slots",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(slots.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode slotArr = objectMapper.readTree(slots.getBody());
        boolean foundSlot = false;
        for (JsonNode s : slotArr) {
            if (practiceId == s.path("practiceId").asLong()
                    && s.path("grade").asInt() == 4
                    && s.path("hasResult").asBoolean()) {
                foundSlot = true;
                break;
            }
        }
        assertThat(foundSlot).as("студент видит оценку 4 за практику в слотах зачётки").isTrue();
    }

    @Test
    @Tag("functional")
    @Order(12)
    @DisplayName("После оценок сводка успеваемости отражает результаты")
    void studentStatisticsReflectsGrades() {
        ResponseEntity<String> r = restTemplate.exchange(
                "/api/v1/statistics/me/student",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("subjectsWithFinalResult");
        assertThat(r.getBody()).contains("practicesWithResult");
    }

    @Test
    @Tag("functional")
    @Order(13)
    @DisplayName("Студент не читает чужой диалог по произвольному UUID")
    void studentCannotReadForeignConversation() {
        ResponseEntity<Void> r = restTemplate.exchange(
                "/api/v1/chats/00000000-0000-0000-0000-000000000001/messages?limit=5",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                Void.class);
        assertThat(r.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @Tag("functional")
    @Order(14)
    @DisplayName("Студент не может смотреть зачётку другого через teacher endpoint")
    void studentCannotUseTeacherGradeEndpoint() {
        ResponseEntity<Void> r = restTemplate.exchange(
                "/api/v1/grades/by-student/" + registeredStudentUserId,
                HttpMethod.GET,
                new HttpEntity<>(jsonHeadersBearer(studentAccessToken)),
                Void.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Tag("functional")
    @Order(15)
    @DisplayName("SUPER_ADMIN: список вузов и доступ к заявкам (область видимости)")
    void superAdminListsUniversitiesAndRegistrationRequests() throws Exception {
        AuthResponse superAuth = login("superadmin@moyvuz.local", "Admin123!");
        HttpHeaders h = jsonHeadersBearer(superAuth.getToken());
        ResponseEntity<String> unis = restTemplate.exchange(
                "/api/v1/universities",
                HttpMethod.GET,
                new HttpEntity<>(h),
                String.class);
        assertThat(unis.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unis.getBody()).contains("Московский");

        ResponseEntity<String> req = restTemplate.exchange(
                "/api/v1/registration-requests?status=PENDING",
                HttpMethod.GET,
                new HttpEntity<>(h),
                String.class);
        assertThat(req.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Tag("functional")
    @Order(16)
    @DisplayName("SUPER_ADMIN отправляет сообщение админу; история читается из Cassandra")
    void sendMessage_andReadHistory() throws Exception {
        AuthResponse superAuth = login("superadmin@moyvuz.local", "Admin123!");
        HttpHeaders headers = jsonHeadersBearer(superAuth.getToken());

        String body = """
                {"recipientId": 1, "text": "integration-test-chat-msg"}
                """;

        ResponseEntity<MessageResponse> sent = restTemplate.exchange(
                "/api/v1/chats/messages",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                MessageResponse.class);
        assertThat(sent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sent.getBody()).isNotNull();
        String convId = sent.getBody().getConversationId();
        assertThat(convId).isNotBlank();

        ResponseEntity<List<MessageResponse>> history = restTemplate.exchange(
                "/api/v1/chats/" + convId + "/messages?limit=20",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });
        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(history.getBody()).isNotNull();
        assertThat(history.getBody().stream().anyMatch(m -> "integration-test-chat-msg".equals(m.getText())))
                .isTrue();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders jsonHeadersBearer(String token) {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private AuthResponse login(String email, String password) throws Exception {
        String jsonBody = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<String> res = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(jsonBody, jsonHeaders()),
                String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readValue(res.getBody(), AuthResponse.class);
    }
}
