package org.example.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.dto.response.AuthResponse;
import org.example.dto.response.MessageResponse;
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
@Tag("functional")
@Tag("regression")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CombinedIntegrationIT extends AbstractIntegrationTest {

    private static String pendingStudentEmail;
    private static String pendingStudentPassword;

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
        assertThat(studentAuth.getEmail()).isEqualTo(pendingStudentEmail);
        assertThat(studentAuth.getUserType().name()).isEqualTo("STUDENT");
    }

    @Test
    @Order(5)
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
