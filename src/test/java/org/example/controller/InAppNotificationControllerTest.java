package org.example.controller;

import org.example.config.SecurityConfig;
import org.example.dto.response.InAppNotificationResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.InAppNotificationQueryService;
import org.example.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InAppNotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InAppNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InAppNotificationQueryService inAppNotificationQueryService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserDetailsService userDetailsService;

    private void mockAuth(String token, String email, String role) {
        var user = new User(email, "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(org.mockito.ArgumentMatchers.eq(token), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
    }

    @Test
    @DisplayName("GET /notifications/my возвращает список")
    void listMy() throws Exception {
        mockAuth("tok", "u@test.ru", "STUDENT");
        Instant t = Instant.parse("2026-01-01T12:00:00Z");
        when(inAppNotificationQueryService.listMine("u@test.ru")).thenReturn(List.of(
                InAppNotificationResponse.builder()
                        .id(1L).kind("GRADE_CHANGED").title("Оценка").body("Тест")
                        .readAt(null).createdAt(t)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/notifications/my")
                        .header("Authorization", "Bearer tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("GRADE_CHANGED"))
                .andExpect(jsonPath("$[0].title").value("Оценка"));
    }

    @Test
    @DisplayName("POST /notifications/{id}/read вызывает сервис")
    void markRead() throws Exception {
        mockAuth("tok", "u@test.ru", "STUDENT");
        mockMvc.perform(post("/api/v1/notifications/5/read")
                        .header("Authorization", "Bearer tok"))
                .andExpect(status().isNoContent());
        verify(inAppNotificationQueryService).markRead(eq(5L), eq("u@test.ru"));
    }
}
