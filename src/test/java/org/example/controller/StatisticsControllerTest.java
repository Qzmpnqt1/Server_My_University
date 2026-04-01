package org.example.controller;

import org.example.config.SecurityConfig;
import org.example.dto.response.*;
import org.example.exception.GlobalExceptionHandler;
import org.example.exception.ResourceNotFoundException;
import org.example.service.JwtService;
import org.example.service.StatisticsService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StatisticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class StatisticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StatisticsService statisticsService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private void mockAuth(String token, String email, String role) {
        User user = new User(email, "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
    }

    // ── Subject statistics — TEACHER/ADMIN ───────────────────────

    @Test
    @DisplayName("GET /statistics/subject/{id} — teacher OK")
    void subjectStats_Teacher_200() throws Exception {
        mockAuth("t", "t@u.ru", "TEACHER");
        when(statisticsService.getSubjectStatistics(eq(1L), eq("t@u.ru"))).thenReturn(
                SubjectStatisticsResponse.builder().subjectDirectionId(1L).subjectName("Математика")
                        .averageGrade(4.0).medianGrade(4.0).creditRate(0.0)
                        .totalStudents(3).gradedStudents(3).missingValues(0)
                        .gradeDistribution(Map.of(4, 2L, 5, 1L)).build());

        mockMvc.perform(get("/api/v1/statistics/subject/1").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectName").value("Математика"))
                .andExpect(jsonPath("$.averageGrade").value(4.0))
                .andExpect(jsonPath("$.medianGrade").value(4.0));
    }

    @Test
    @DisplayName("GET /statistics/subject/{id} — student 403")
    void subjectStats_Student_403() throws Exception {
        mockAuth("s", "s@u.ru", "STUDENT");
        mockMvc.perform(get("/api/v1/statistics/subject/1").header("Authorization", "Bearer s"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /statistics/subject/{id} — no auth 403")
    void subjectStats_NoAuth_403() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/subject/1"))
                .andExpect(status().isForbidden());
    }

    // ── Practice statistics — TEACHER/ADMIN ──────────────────────

    @Test
    @DisplayName("GET /statistics/practices/{id} — admin OK")
    void practiceStats_Admin_200() throws Exception {
        mockAuth("a", "a@u.ru", "ADMIN");
        when(statisticsService.getPracticeStatistics(eq(1L), eq("a@u.ru"))).thenReturn(
                PracticeStatisticsResponse.builder().subjectDirectionId(1L).subjectName("Мат")
                        .overallProgress(75.0).totalScoreAverage(7.0).completionPercentage(75.0)
                        .totalPractices(2).countedValues(4).missingValues(1)
                        .practices(List.of()).build());

        mockMvc.perform(get("/api/v1/statistics/practices/1").header("Authorization", "Bearer a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallProgress").value(75.0));
    }

    @Test
    @DisplayName("GET /statistics/practices/{id} — student 403")
    void practiceStats_Student_403() throws Exception {
        mockAuth("s", "s@u.ru", "STUDENT");
        mockMvc.perform(get("/api/v1/statistics/practices/1").header("Authorization", "Bearer s"))
                .andExpect(status().isForbidden());
    }

    // ── Group statistics — TEACHER/ADMIN ─────────────────────────

    @Test
    @DisplayName("GET /statistics/group/{id} — teacher OK")
    void groupStats_Teacher_200() throws Exception {
        mockAuth("t", "t@u.ru", "TEACHER");
        when(statisticsService.getGroupStatistics(eq(1L), eq("t@u.ru"))).thenReturn(
                GroupStatisticsResponse.builder().groupId(1L).groupName("ИТ-201")
                        .averagePerformance(4.0).debtRate(25.0).studentCount(4).studentsWithDebt(1)
                        .countedValues(4).missingValues(0).averageBySubject(Map.of()).build());

        mockMvc.perform(get("/api/v1/statistics/group/1").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("ИТ-201"))
                .andExpect(jsonPath("$.debtRate").value(25.0));
    }

    @Test
    @DisplayName("GET /statistics/group/{id} — student 403")
    void groupStats_Student_403() throws Exception {
        mockAuth("s", "s@u.ru", "STUDENT");
        mockMvc.perform(get("/api/v1/statistics/group/1").header("Authorization", "Bearer s"))
                .andExpect(status().isForbidden());
    }

    // ── Direction statistics — TEACHER/ADMIN ─────────────────────

    @Test
    @DisplayName("GET /statistics/direction/{id} — admin OK")
    void directionStats_Admin_200() throws Exception {
        mockAuth("a", "a@u.ru", "ADMIN");
        when(statisticsService.getDirectionStatistics(eq(1L), eq("a@u.ru"))).thenReturn(
                DirectionStatisticsResponse.builder().directionId(1L).directionName("Инф")
                        .averagePerformance(4.2).debtRate(10.0).totalStudents(20).studentsWithDebt(2)
                        .groupCount(2).groups(List.of()).build());

        mockMvc.perform(get("/api/v1/statistics/direction/1").header("Authorization", "Bearer a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directionName").value("Инф"));
    }

    @Test
    @DisplayName("GET /statistics/direction/{id} — student 403")
    void directionStats_Student_403() throws Exception {
        mockAuth("s", "s@u.ru", "STUDENT");
        mockMvc.perform(get("/api/v1/statistics/direction/1").header("Authorization", "Bearer s"))
                .andExpect(status().isForbidden());
    }

    // ── Institute statistics — ADMIN only ────────────────────────

    @Test
    @DisplayName("GET /statistics/institute/{id} — admin OK")
    void instituteStats_Admin_200() throws Exception {
        mockAuth("a", "a@u.ru", "ADMIN");
        when(statisticsService.getInstituteStatistics(eq(1L), eq("a@u.ru"))).thenReturn(
                InstituteStatisticsResponse.builder().instituteId(1L).instituteName("ИТИ")
                        .averagePerformance(3.8).debtRate(15.0).totalStudents(50).studentsWithDebt(8)
                        .directionCount(3).directions(List.of()).build());

        mockMvc.perform(get("/api/v1/statistics/institute/1").header("Authorization", "Bearer a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instituteName").value("ИТИ"));
    }

    @Test
    @DisplayName("GET /statistics/institute/{id} — teacher 403")
    void instituteStats_Teacher_403() throws Exception {
        mockAuth("t", "t@u.ru", "TEACHER");
        mockMvc.perform(get("/api/v1/statistics/institute/1").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
    }

    // ── University statistics — ADMIN only ───────────────────────

    @Test
    @DisplayName("GET /statistics/university/{id} — admin OK")
    void universityStats_Admin_200() throws Exception {
        mockAuth("a", "a@u.ru", "ADMIN");
        when(statisticsService.getUniversityStatistics(eq(1L), eq("a@u.ru"))).thenReturn(
                UniversityStatisticsResponse.builder().universityId(1L).universityName("МГУ")
                        .averagePerformance(3.9).debtRate(12.0).totalStudents(200).studentsWithDebt(24)
                        .instituteCount(4).institutes(List.of()).build());

        mockMvc.perform(get("/api/v1/statistics/university/1").header("Authorization", "Bearer a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.universityName").value("МГУ"));
    }

    @Test
    @DisplayName("GET /statistics/university/{id} — teacher 403")
    void universityStats_Teacher_403() throws Exception {
        mockAuth("t", "t@u.ru", "TEACHER");
        mockMvc.perform(get("/api/v1/statistics/university/1").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
    }

    // ── Schedule stats — mixed access ────────────────────────────

    @Test
    @DisplayName("GET /statistics/schedule/teacher/{id} — teacher OK")
    void teacherSchedule_Teacher_200() throws Exception {
        mockAuth("t", "t@u.ru", "TEACHER");
        when(statisticsService.getTeacherScheduleStatistics(eq(1L), eq("t@u.ru"))).thenReturn(
                ScheduleStatisticsResponse.builder().scope("teacher").entityId(1L)
                        .totalLessons(5).totalHours(7.5).byDayOfWeek(Map.of()).byWeekNumber(Map.of()).build());

        mockMvc.perform(get("/api/v1/statistics/schedule/teacher/1").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLessons").value(5));
    }

    @Test
    @DisplayName("GET /statistics/schedule/teacher/{id} — student 403")
    void teacherSchedule_Student_403() throws Exception {
        mockAuth("s", "s@u.ru", "STUDENT");
        mockMvc.perform(get("/api/v1/statistics/schedule/teacher/1").header("Authorization", "Bearer s"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /statistics/schedule/group/{id} — admin OK")
    void groupSchedule_Admin_200() throws Exception {
        mockAuth("a", "admin@u.ru", "ADMIN");
        when(statisticsService.getGroupScheduleStatistics(eq(1L), eq("admin@u.ru"))).thenReturn(
                ScheduleStatisticsResponse.builder().scope("group").entityId(1L)
                        .totalLessons(3).totalHours(4.5).byDayOfWeek(Map.of()).byWeekNumber(Map.of()).build());

        mockMvc.perform(get("/api/v1/statistics/schedule/group/1")
                        .header("Authorization", "Bearer a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("group"));
    }

    @Test
    @DisplayName("GET /statistics/schedule/classroom/{id} — teacher OK")
    void classroomSchedule_Teacher_200() throws Exception {
        mockAuth("t", "t@u.ru", "TEACHER");
        when(statisticsService.getClassroomScheduleStatistics(eq(1L), eq("t@u.ru"))).thenReturn(
                ScheduleStatisticsResponse.builder().scope("classroom").entityId(1L)
                        .totalLessons(0).totalHours(0.0).byDayOfWeek(Map.of()).byWeekNumber(Map.of()).build());

        mockMvc.perform(get("/api/v1/statistics/schedule/classroom/1")
                        .header("Authorization", "Bearer t"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /statistics/subject/{id} — not found returns 404")
    void subjectStats_NotFound_404() throws Exception {
        mockAuth("t", "t@u.ru", "TEACHER");
        when(statisticsService.getSubjectStatistics(eq(99L), eq("t@u.ru")))
                .thenThrow(new ResourceNotFoundException("Предмет в направлении не найден"));

        mockMvc.perform(get("/api/v1/statistics/subject/99").header("Authorization", "Bearer t"))
                .andExpect(status().isNotFound());
    }
}
