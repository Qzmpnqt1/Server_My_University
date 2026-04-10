package org.example.controller;

import io.jsonwebtoken.JwtException;
import org.example.config.SecurityConfig;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("security")
@WebMvcTest(controllers = StatisticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class StatisticsSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /statistics/me/student без токена — 401/403")
    void studentSummary_anonymous_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/me/student"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc != 401 && sc != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + sc);
                    }
                });
    }

    @Test
    @DisplayName("Невалидный JWT на /statistics/me/student — 401")
    void studentSummary_invalidJwt_rejected() throws Exception {
        when(jwtService.extractUsername(anyString())).thenThrow(new JwtException("invalid"));
        mockMvc.perform(get("/api/v1/statistics/me/student")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("STUDENT не может GET /statistics/subject/{id} (агрегаты преподавателя)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotReadSubjectStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/subject/1"))
                .andExpect(status().isForbidden());
    }
}
