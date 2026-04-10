package org.example.controller;

import org.example.config.SecurityConfig;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.GradeService;
import org.example.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Негативная безопасность: зачётка студента только для аутентифицированного STUDENT.
 */
@Tag("security")
@WebMvcTest(controllers = GradeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GradeSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GradeService gradeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /grades/my без токена — 401/403")
    void myGrades_anonymous_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/grades/my"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc != 401 && sc != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + sc);
                    }
                });
    }

    @Test
    @DisplayName("STUDENT не может POST /grades (только TEACHER/ADMIN)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotCreateGrade() throws Exception {
        mockMvc.perform(post("/api/v1/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"subjectDirectionId\":1,\"grade\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT не может читать зачётку другого через GET /grades/by-student/{id}")
    @WithMockUser(roles = "STUDENT")
    void studentCannotReadOtherStudentGradebook() throws Exception {
        mockMvc.perform(get("/api/v1/grades/by-student/999"))
                .andExpect(status().isForbidden());
    }
}
