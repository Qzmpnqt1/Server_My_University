package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.SubjectPracticeRequest;
import org.example.dto.response.SubjectPracticeResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.exception.ResourceNotFoundException;
import org.example.service.JwtService;
import org.example.service.SubjectPracticeService;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubjectPracticeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SubjectPracticeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SubjectPracticeService subjectPracticeService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private void mockAuth(String token, String email, String role) {
        User user = new User(email, "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
    }

    private SubjectPracticeResponse sampleResponse() {
        return SubjectPracticeResponse.builder()
                .id(1L).subjectDirectionId(1L).practiceNumber(1)
                .practiceTitle("Лабораторная 1").maxGrade(10).isCredit(false).build();
    }

    private SubjectPracticeRequest sampleRequest() {
        return SubjectPracticeRequest.builder()
                .subjectDirectionId(1L).practiceNumber(1)
                .practiceTitle("Лабораторная 1").maxGrade(10).isCredit(false).build();
    }

    // ── GET — authenticated ──────────────────────────────────────

    @Test
    @DisplayName("GET /subject-practices?subjectDirectionId=1 — authenticated")
    void getByDirection_Auth_200() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        when(subjectPracticeService.getBySubjectDirection(eq(1L), eq("admin@uni.ru")))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/subject-practices")
                        .param("subjectDirectionId", "1")
                        .header("Authorization", "Bearer a-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].practiceTitle").value("Лабораторная 1"))
                .andExpect(jsonPath("$[0].maxGrade").value(10));
    }

    @Test
    @DisplayName("GET /subject-practices/{id} — authenticated")
    void getById_Auth_200() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        when(subjectPracticeService.getById(eq(1L), eq("admin@uni.ru"))).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/subject-practices/1")
                        .header("Authorization", "Bearer a-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practiceTitle").value("Лабораторная 1"));
    }

    @Test
    @DisplayName("GET /subject-practices/{id} — not found returns 404")
    void getById_NotFound_404() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        when(subjectPracticeService.getById(eq(99L), eq("admin@uni.ru")))
                .thenThrow(new ResourceNotFoundException("SubjectPractice not found with id: 99"));

        mockMvc.perform(get("/api/v1/subject-practices/99")
                        .header("Authorization", "Bearer a-token"))
                .andExpect(status().isNotFound());
    }

    // ── POST — ADMIN only ────────────────────────────────────────

    @Test
    @DisplayName("POST /subject-practices — admin creates")
    void create_Admin_201() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        when(subjectPracticeService.create(any(SubjectPracticeRequest.class), eq("admin@uni.ru")))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/subject-practices")
                        .header("Authorization", "Bearer a-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.practiceTitle").value("Лабораторная 1"));
    }

    @Test
    @DisplayName("POST /subject-practices — teacher returns 403")
    void create_Teacher_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");

        mockMvc.perform(post("/api/v1/subject-practices")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /subject-practices — student returns 403")
    void create_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(post("/api/v1/subject-practices")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /subject-practices — missing fields returns 400")
    void create_MissingFields_400() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");

        mockMvc.perform(post("/api/v1/subject-practices")
                        .header("Authorization", "Bearer a-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── PUT — ADMIN only ─────────────────────────────────────────

    @Test
    @DisplayName("PUT /subject-practices/{id} — admin updates")
    void update_Admin_200() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        when(subjectPracticeService.update(eq(1L), any(SubjectPracticeRequest.class), eq("admin@uni.ru")))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/subject-practices/1")
                        .header("Authorization", "Bearer a-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /subject-practices/{id} — teacher returns 403")
    void update_Teacher_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");

        mockMvc.perform(put("/api/v1/subject-practices/1")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    // ── DELETE — ADMIN only ──────────────────────────────────────

    @Test
    @DisplayName("DELETE /subject-practices/{id} — admin deletes")
    void delete_Admin_204() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        doNothing().when(subjectPracticeService).delete(eq(1L), eq("admin@uni.ru"));

        mockMvc.perform(delete("/api/v1/subject-practices/1")
                        .header("Authorization", "Bearer a-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /subject-practices/{id} — student returns 403")
    void delete_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(delete("/api/v1/subject-practices/1")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /subject-practices/{id} — not found returns 404")
    void delete_NotFound_404() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        doThrow(new ResourceNotFoundException("SubjectPractice not found with id: 99"))
                .when(subjectPracticeService).delete(eq(99L), eq("admin@uni.ru"));

        mockMvc.perform(delete("/api/v1/subject-practices/99")
                        .header("Authorization", "Bearer a-token"))
                .andExpect(status().isNotFound());
    }
}
