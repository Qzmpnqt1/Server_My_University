package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.GradeRequest;
import org.example.dto.response.GradeResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.GradeService;
import org.example.service.JwtService;
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

@WebMvcTest(controllers = GradeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GradeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private GradeService gradeService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private void mockAuth(String token, String email, String role) {
        User user = new User(email, "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
    }

    private GradeResponse sampleResponse() {
        return GradeResponse.builder()
                .id(1L).studentId(3L).studentName("Иванов Иван")
                .subjectDirectionId(1L).subjectName("Математика")
                .grade(5).creditStatus(null).build();
    }

    // ── GET /my — STUDENT only ───────────────────────────────────

    @Test
    @DisplayName("GET /grades/my — student sees own grades")
    void getMyGrades_Student_200() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        when(gradeService.getMyGrades("student@uni.ru")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/grades/my")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectName").value("Математика"))
                .andExpect(jsonPath("$[0].grade").value(5));
    }

    @Test
    @DisplayName("GET /grades/my — teacher returns 403")
    void getMyGrades_Teacher_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");

        mockMvc.perform(get("/api/v1/grades/my")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /grades/my — no auth returns 403")
    void getMyGrades_NoAuth_403() throws Exception {
        mockMvc.perform(get("/api/v1/grades/my"))
                .andExpect(status().isForbidden());
    }

    // ── GET /by-student/{id} — TEACHER/ADMIN ─────────────────────

    @Test
    @DisplayName("GET /grades/by-student/{id} — admin sees any student")
    void getByStudent_Admin_200() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        when(gradeService.getByStudent(eq(3L), eq("admin@uni.ru"))).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/grades/by-student/3")
                        .header("Authorization", "Bearer a-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(3));
    }

    @Test
    @DisplayName("GET /grades/by-student/{id} — teacher sees student")
    void getByStudent_Teacher_200() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(gradeService.getByStudent(eq(3L), eq("teacher@uni.ru"))).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/grades/by-student/3")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /grades/by-student/{id} — student returns 403")
    void getByStudent_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(get("/api/v1/grades/by-student/3")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isForbidden());
    }

    // ── GET /by-subject-direction/{id} — TEACHER/ADMIN ───────────

    @Test
    @DisplayName("GET /grades/by-subject-direction/{id} — teacher sees grades")
    void getBySubjectDirection_Teacher_200() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(gradeService.getBySubjectDirection(eq(1L), eq("teacher@uni.ru")))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/grades/by-subject-direction/1")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /grades/by-subject-direction/{id} — non-owner teacher gets 403 from service")
    void getBySubjectDirection_NonOwner_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(gradeService.getBySubjectDirection(eq(1L), eq("teacher@uni.ru")))
                .thenThrow(new AccessDeniedException("Преподаватель не назначен на данный предмет"));

        mockMvc.perform(get("/api/v1/grades/by-subject-direction/1")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Преподаватель не назначен на данный предмет"));
    }

    // ── POST /grades — TEACHER/ADMIN ─────────────────────────────

    @Test
    @DisplayName("POST /grades — teacher creates grade")
    void create_Teacher_201() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(5).build();
        when(gradeService.create(any(GradeRequest.class), eq("teacher@uni.ru")))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grade").value(5));
    }

    @Test
    @DisplayName("POST /grades — student returns 403")
    void create_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(post("/api/v1/grades")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GradeRequest.builder().studentId(3L).subjectDirectionId(1L).grade(5).build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /grades — duplicate returns 409")
    void create_Duplicate_409() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(gradeService.create(any(GradeRequest.class), eq("teacher@uni.ru")))
                .thenThrow(new ConflictException("Оценка для данного студента по данному предмету уже существует"));

        mockMvc.perform(post("/api/v1/grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GradeRequest.builder().studentId(3L).subjectDirectionId(1L).grade(5).build())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /grades — grade out of range returns 400")
    void create_GradeOutOfRange_400() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(gradeService.create(any(GradeRequest.class), eq("teacher@uni.ru")))
                .thenThrow(new BadRequestException("Итоговая оценка должна быть в диапазоне от 1 до 5"));

        mockMvc.perform(post("/api/v1/grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GradeRequest.builder().studentId(3L).subjectDirectionId(1L).grade(6).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Итоговая оценка должна быть в диапазоне от 1 до 5"));
    }

    @Test
    @DisplayName("POST /grades — missing fields returns 400")
    void create_MissingFields_400() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");

        mockMvc.perform(post("/api/v1/grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /grades/{id} — TEACHER/ADMIN ─────────────────────────

    @Test
    @DisplayName("PUT /grades/{id} — teacher updates grade")
    void update_Teacher_200() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(gradeService.update(eq(1L), any(GradeRequest.class), eq("teacher@uni.ru")))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/grades/1")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GradeRequest.builder().studentId(3L).subjectDirectionId(1L).grade(4).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /grades/{id} — student returns 403")
    void update_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(put("/api/v1/grades/1")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GradeRequest.builder().studentId(3L).subjectDirectionId(1L).grade(4).build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /grades/{id} — non-owner teacher gets 403 from service")
    void update_NonOwner_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(gradeService.update(eq(1L), any(GradeRequest.class), eq("teacher@uni.ru")))
                .thenThrow(new AccessDeniedException("Преподаватель не назначен на данный предмет"));

        mockMvc.perform(put("/api/v1/grades/1")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GradeRequest.builder().studentId(3L).subjectDirectionId(1L).grade(4).build())))
                .andExpect(status().isForbidden());
    }
}
