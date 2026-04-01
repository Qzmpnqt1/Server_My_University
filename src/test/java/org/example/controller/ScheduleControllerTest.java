package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.ScheduleRequest;
import org.example.dto.response.ScheduleResponse;
import org.example.exception.ConflictException;
import org.example.exception.GlobalExceptionHandler;
import org.example.exception.ResourceNotFoundException;
import org.example.model.LessonType;
import org.example.service.JwtService;
import org.example.service.ScheduleAuthorizationService;
import org.example.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ScheduleController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ScheduleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ScheduleService scheduleService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private ScheduleAuthorizationService scheduleAuthorizationService;

    @BeforeEach
    void scheduleAuthzLenient() {
        lenient().doNothing().when(scheduleAuthorizationService).ensureAdmin(anyString());
        lenient().doNothing().when(scheduleAuthorizationService).ensureCanViewGroupSchedule(anyString(), anyLong());
        lenient().doNothing().when(scheduleAuthorizationService).ensureCanViewTeacherSchedule(anyString(), anyLong());
        lenient().doNothing().when(scheduleAuthorizationService).ensureCanViewScheduleEntry(anyString(), anyLong());
    }

    private void mockAdminAuth() {
        User admin = new User("admin@uni.ru", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(jwtService.extractUsername("admin-token")).thenReturn("admin@uni.ru");
        when(jwtService.isTokenValid(eq("admin-token"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin@uni.ru")).thenReturn(admin);
    }

    private void mockStudentAuth() {
        User student = new User("student@uni.ru", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(jwtService.extractUsername("student-token")).thenReturn("student@uni.ru");
        when(jwtService.isTokenValid(eq("student-token"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("student@uni.ru")).thenReturn(student);
    }

    private void mockTeacherAuth() {
        User teacher = new User("teacher@uni.ru", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        when(jwtService.extractUsername("teacher-token")).thenReturn("teacher@uni.ru");
        when(jwtService.isTokenValid(eq("teacher-token"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("teacher@uni.ru")).thenReturn(teacher);
    }

    private ScheduleResponse sampleResponse() {
        return ScheduleResponse.builder()
                .id(1L).subjectTypeId(1L).subjectName("Математика").lessonType(LessonType.LECTURE)
                .teacherId(1L).teacherName("Попов Алексей").groupId(1L).groupName("ИТ-201")
                .classroomId(1L).classroomInfo("Корпус А, ауд. 305")
                .dayOfWeek(1).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30)).weekNumber(1)
                .build();
    }

    private ScheduleRequest sampleRequest() {
        return ScheduleRequest.builder()
                .subjectTypeId(1L).teacherId(1L).groupId(1L).classroomId(1L)
                .dayOfWeek(1).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30)).weekNumber(1)
                .build();
    }

    // ── Authenticated GET endpoints ──────────────────────────────

    @Test
    @DisplayName("GET /api/v1/schedule — admin, returns all")
    void getAll_Admin_200() throws Exception {
        mockAdminAuth();
        when(scheduleService.getAllForAdmin(eq("admin@uni.ru"))).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectName").value("Математика"))
                .andExpect(jsonPath("$[0].dayOfWeek").value(1));

        verify(scheduleAuthorizationService).ensureAdmin("admin@uni.ru");
    }

    @Test
    @DisplayName("GET /api/v1/schedule?groupId=1 — filters by group")
    void getAll_GroupFilter_200() throws Exception {
        mockStudentAuth();
        when(scheduleService.getByGroup(eq(1L), isNull(), isNull())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule").param("groupId", "1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(1));

        verify(scheduleService).getByGroup(1L, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/schedule?teacherId=1&weekNumber=1 — filters by teacher+week")
    void getAll_TeacherWeekFilter_200() throws Exception {
        mockTeacherAuth();
        when(scheduleService.getByTeacher(eq(1L), eq(1), isNull())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule")
                        .param("teacherId", "1")
                        .param("weekNumber", "1")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk());

        verify(scheduleService).getByTeacher(1L, 1, null);
    }

    @Test
    @DisplayName("GET /api/v1/schedule?groupId=1&weekNumber=1&dayOfWeek=1 — all filters")
    void getAll_AllFilters_200() throws Exception {
        mockStudentAuth();
        when(scheduleService.getByGroup(eq(1L), eq(1), eq(1))).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule")
                        .param("groupId", "1")
                        .param("weekNumber", "1")
                        .param("dayOfWeek", "1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk());

        verify(scheduleService).getByGroup(1L, 1, 1);
    }

    @Test
    @DisplayName("GET /api/v1/schedule/group/{groupId} — authenticated")
    void getByGroup_Auth_200() throws Exception {
        mockStudentAuth();
        when(scheduleService.getByGroup(eq(1L), isNull(), isNull())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule/group/1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupName").value("ИТ-201"));
    }

    @Test
    @DisplayName("GET /api/v1/schedule/group/{groupId}?weekNumber=1&dayOfWeek=2 — with filters")
    void getByGroup_WithFilters_200() throws Exception {
        mockStudentAuth();
        when(scheduleService.getByGroup(eq(1L), eq(1), eq(2))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/schedule/group/1")
                        .param("weekNumber", "1")
                        .param("dayOfWeek", "2")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(scheduleService).getByGroup(1L, 1, 2);
    }

    @Test
    @DisplayName("GET /api/v1/schedule/teacher/{teacherId} — authenticated")
    void getByTeacher_Auth_200() throws Exception {
        mockTeacherAuth();
        when(scheduleService.getByTeacher(eq(1L), isNull(), isNull())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule/teacher/1")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teacherName").value("Попов Алексей"));
    }

    @Test
    @DisplayName("GET /api/v1/schedule/{id} — authenticated")
    void getById_Auth_200() throws Exception {
        mockStudentAuth();
        when(scheduleService.getById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/schedule/1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.subjectName").value("Математика"));
    }

    @Test
    @DisplayName("GET /api/v1/schedule/{id} — not found returns 404")
    void getById_NotFound_404() throws Exception {
        mockStudentAuth();
        when(scheduleService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Запись расписания не найдена с id: 99"));

        mockMvc.perform(get("/api/v1/schedule/99")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isNotFound());
    }

    // ── /schedule/my — requires auth ─────────────────────────────

    @Test
    @DisplayName("GET /api/v1/schedule/my — student sees own schedule")
    void getMySchedule_Student_200() throws Exception {
        mockStudentAuth();
        when(scheduleService.getMySchedule(eq("student@uni.ru"), isNull(), isNull()))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule/my")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupName").value("ИТ-201"));
    }

    @Test
    @DisplayName("GET /api/v1/schedule/my — teacher sees own schedule")
    void getMySchedule_Teacher_200() throws Exception {
        mockTeacherAuth();
        when(scheduleService.getMySchedule(eq("teacher@uni.ru"), isNull(), isNull()))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/schedule/my")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/schedule/my — with week and day filters")
    void getMySchedule_WithFilters_200() throws Exception {
        mockStudentAuth();
        when(scheduleService.getMySchedule(eq("student@uni.ru"), eq(1), eq(3)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/schedule/my")
                        .header("Authorization", "Bearer student-token")
                        .param("weekNumber", "1")
                        .param("dayOfWeek", "3"))
                .andExpect(status().isOk());

        verify(scheduleService).getMySchedule("student@uni.ru", 1, 3);
    }

    @Test
    @DisplayName("GET /api/v1/schedule/my — no auth returns 403")
    void getMySchedule_NoAuth_403() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/schedule/my — admin returns 403 (only STUDENT/TEACHER)")
    void getMySchedule_Admin_403() throws Exception {
        mockAdminAuth();

        mockMvc.perform(get("/api/v1/schedule/my")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
    }

    // ── CRUD — admin only ────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/schedule — admin creates schedule")
    void create_Admin_201() throws Exception {
        mockAdminAuth();
        when(scheduleService.create(any(ScheduleRequest.class), eq("admin@uni.ru"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/schedule")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.subjectName").value("Математика"));
    }

    @Test
    @DisplayName("POST /api/v1/schedule — student returns 403")
    void create_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(post("/api/v1/schedule")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/schedule — teacher returns 403")
    void create_Teacher_403() throws Exception {
        mockTeacherAuth();

        mockMvc.perform(post("/api/v1/schedule")
                        .header("Authorization", "Bearer teacher-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/schedule — conflict returns 409")
    void create_Conflict_409() throws Exception {
        mockAdminAuth();
        when(scheduleService.create(any(ScheduleRequest.class), eq("admin@uni.ru")))
                .thenThrow(new ConflictException("Конфликт расписания: преподаватель уже занят в указанное время"));

        mockMvc.perform(post("/api/v1/schedule")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Конфликт расписания: преподаватель уже занят в указанное время"));
    }

    @Test
    @DisplayName("POST /api/v1/schedule — missing fields returns 400")
    void create_MissingFields_400() throws Exception {
        mockAdminAuth();

        mockMvc.perform(post("/api/v1/schedule")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/schedule/{id} — admin updates schedule")
    void update_Admin_200() throws Exception {
        mockAdminAuth();
        when(scheduleService.update(eq(1L), any(ScheduleRequest.class), eq("admin@uni.ru"))).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/schedule/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/schedule/{id} — student returns 403")
    void update_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(put("/api/v1/schedule/1")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/schedule/{id} — admin deletes schedule")
    void delete_Admin_204() throws Exception {
        mockAdminAuth();
        doNothing().when(scheduleService).delete(eq(1L), eq("admin@uni.ru"));

        mockMvc.perform(delete("/api/v1/schedule/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(scheduleService).delete(1L, "admin@uni.ru");
    }

    @Test
    @DisplayName("DELETE /api/v1/schedule/{id} — student returns 403")
    void delete_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(delete("/api/v1/schedule/1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/schedule/{id} — not found returns 404")
    void delete_NotFound_404() throws Exception {
        mockAdminAuth();
        doThrow(new ResourceNotFoundException("Запись расписания не найдена"))
                .when(scheduleService).delete(eq(99L), eq("admin@uni.ru"));

        mockMvc.perform(delete("/api/v1/schedule/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound());
    }
}
