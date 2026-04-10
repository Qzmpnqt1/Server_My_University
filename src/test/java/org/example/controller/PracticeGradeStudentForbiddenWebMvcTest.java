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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("security")
@WebMvcTest(controllers = PracticeGradeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PracticeGradeStudentForbiddenWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PracticeGradeService practiceGradeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /practice-grades с токеном STUDENT — 403 (не TEACHER/ADMIN)")
    void studentRole_cannotCreatePracticeGrade() throws Exception {
        User u = new User("st@t.ru", "p", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(jwtService.extractUsername("student-jwt")).thenReturn("st@t.ru");
        when(jwtService.isTokenValid(eq("student-jwt"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("st@t.ru")).thenReturn(u);

        mockMvc.perform(post("/api/v1/practice-grades")
                        .header("Authorization", "Bearer student-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"practiceId\":1,\"grade\":4}"))
                .andExpect(status().isForbidden());
    }
}
