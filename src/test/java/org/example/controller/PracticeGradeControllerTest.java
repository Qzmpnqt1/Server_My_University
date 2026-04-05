package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.PracticeGradeRequest;
import org.example.dto.response.PracticeGradeResponse;
import org.example.dto.response.StudentPracticeSlotResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.PracticeGradeService;
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

@WebMvcTest(controllers = PracticeGradeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PracticeGradeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PracticeGradeService practiceGradeService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private void mockAuth(String token, String email, String role) {
        User user = new User(email, "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
    }

    private PracticeGradeResponse sampleResponse() {
        return PracticeGradeResponse.builder()
                .id(1L).studentId(3L).studentName("Иванов Иван")
                .practiceId(1L).practiceTitle("Лабораторная 1").practiceNumber(1)
                .grade(8).creditStatus(null).maxGrade(10).practiceIsCredit(false).build();
    }

    // ── GET /my/subject/{id}/slots — STUDENT only ────────────────

    @Test
    @DisplayName("GET /practice-grades/my/subject/1/slots — student sees slots")
    void getMySlots_Student_200() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        StudentPracticeSlotResponse slot = StudentPracticeSlotResponse.builder()
                .practiceId(1L).practiceNumber(1).practiceTitle("Лаб 1")
                .maxGrade(10).isCredit(false).grade(8).creditStatus(null).hasResult(true)
                .build();
        when(practiceGradeService.getMyPracticeSlotsForSubject(eq("student@uni.ru"), eq(1L)))
                .thenReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/practice-grades/my/subject/1/slots")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].practiceTitle").value("Лаб 1"))
                .andExpect(jsonPath("$[0].hasResult").value(true));

        verify(practiceGradeService).getMyPracticeSlotsForSubject("student@uni.ru", 1L);
    }

    @Test
    @DisplayName("GET /practice-grades/my/subject/1/slots — teacher 403")
    void getMySlots_Teacher_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        mockMvc.perform(get("/api/v1/practice-grades/my/subject/1/slots")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isForbidden());
    }

    // ── GET /my — STUDENT only ───────────────────────────────────

    @Test
    @DisplayName("GET /practice-grades/my — student sees own grades")
    void getMyGrades_Student_200() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        when(practiceGradeService.getMyPracticeGrades(eq("student@uni.ru"), isNull()))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/practice-grades/my")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].practiceTitle").value("Лабораторная 1"))
                .andExpect(jsonPath("$[0].maxGrade").value(10));
    }

    @Test
    @DisplayName("GET /practice-grades/my?subjectDirectionId=1 — filter works")
    void getMyGrades_WithFilter_200() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        when(practiceGradeService.getMyPracticeGrades(eq("student@uni.ru"), eq(1L)))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/practice-grades/my")
                        .header("Authorization", "Bearer s-token")
                        .param("subjectDirectionId", "1"))
                .andExpect(status().isOk());

        verify(practiceGradeService).getMyPracticeGrades("student@uni.ru", 1L);
    }

    @Test
    @DisplayName("GET /practice-grades/my — teacher returns 403")
    void getMyGrades_Teacher_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");

        mockMvc.perform(get("/api/v1/practice-grades/my")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /practice-grades/my — no auth returns 403")
    void getMyGrades_NoAuth_403() throws Exception {
        mockMvc.perform(get("/api/v1/practice-grades/my"))
                .andExpect(status().isForbidden());
    }

    // ── GET /by-practice/{id} — TEACHER/ADMIN ────────────────────

    @Test
    @DisplayName("GET /practice-grades/by-practice/{id} — teacher sees grades")
    void getByPractice_Teacher_200() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(practiceGradeService.getByPractice(eq(1L), eq("teacher@uni.ru")))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/practice-grades/by-practice/1")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grade").value(8));
    }

    @Test
    @DisplayName("GET /practice-grades/by-practice/{id} — student returns 403")
    void getByPractice_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(get("/api/v1/practice-grades/by-practice/1")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /practice-grades/by-practice/{id} — non-owner teacher gets 403 from service")
    void getByPractice_NonOwner_403() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(practiceGradeService.getByPractice(eq(1L), eq("teacher@uni.ru")))
                .thenThrow(new AccessDeniedException("Преподаватель не назначен на данный предмет"));

        mockMvc.perform(get("/api/v1/practice-grades/by-practice/1")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isForbidden());
    }

    // ── POST /practice-grades — TEACHER/ADMIN ────────────────────

    @Test
    @DisplayName("POST /practice-grades — teacher creates grade")
    void create_Teacher_201() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(practiceGradeService.create(any(PracticeGradeRequest.class), eq("teacher@uni.ru")))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/practice-grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PracticeGradeRequest.builder().studentId(3L).practiceId(1L).grade(8).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grade").value(8));
    }

    @Test
    @DisplayName("POST /practice-grades — student returns 403")
    void create_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(post("/api/v1/practice-grades")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PracticeGradeRequest.builder().studentId(3L).practiceId(1L).grade(8).build())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /practice-grades — grade exceeds max_grade returns 400")
    void create_ExceedsMax_400() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(practiceGradeService.create(any(PracticeGradeRequest.class), eq("teacher@uni.ru")))
                .thenThrow(new BadRequestException("Оценка должна быть в диапазоне от 0 до 10"));

        mockMvc.perform(post("/api/v1/practice-grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PracticeGradeRequest.builder().studentId(3L).practiceId(1L).grade(15).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Оценка должна быть в диапазоне от 0 до 10"));
    }

    @Test
    @DisplayName("POST /practice-grades — credit practice with numeric grade returns 400")
    void create_CreditNumeric_400() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(practiceGradeService.create(any(PracticeGradeRequest.class), eq("teacher@uni.ru")))
                .thenThrow(new BadRequestException(
                        "Для зачётной практики нельзя указывать числовую оценку, используйте creditStatus"));

        mockMvc.perform(post("/api/v1/practice-grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PracticeGradeRequest.builder().studentId(3L).practiceId(2L).grade(5).build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /practice-grades — duplicate returns 409")
    void create_Duplicate_409() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(practiceGradeService.create(any(PracticeGradeRequest.class), eq("teacher@uni.ru")))
                .thenThrow(new ConflictException("Оценка за данную практику для данного студента уже существует"));

        mockMvc.perform(post("/api/v1/practice-grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PracticeGradeRequest.builder().studentId(3L).practiceId(1L).grade(8).build())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /practice-grades — missing fields returns 400")
    void create_MissingFields_400() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");

        mockMvc.perform(post("/api/v1/practice-grades")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /practice-grades/{id} — TEACHER/ADMIN ────────────────

    @Test
    @DisplayName("PUT /practice-grades/{id} — teacher updates grade")
    void update_Teacher_200() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(practiceGradeService.update(eq(1L), any(PracticeGradeRequest.class), eq("teacher@uni.ru")))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/practice-grades/1")
                        .header("Authorization", "Bearer t-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PracticeGradeRequest.builder().studentId(3L).practiceId(1L).grade(9).build())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /practice-grades/{id} — student returns 403")
    void update_Student_403() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");

        mockMvc.perform(put("/api/v1/practice-grades/1")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PracticeGradeRequest.builder().studentId(3L).practiceId(1L).grade(9).build())))
                .andExpect(status().isForbidden());
    }
}
