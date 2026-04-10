package org.example.controller;

import org.example.config.SecurityConfig;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.PracticeGradeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Tag("security")
@WebMvcTest(controllers = PracticeGradeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PracticeGradeSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PracticeGradeService practiceGradeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /practice-grades без токена — 401/403")
    void create_anonymous_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/practice-grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc != 401 && sc != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + sc);
                    }
                });
    }
}
