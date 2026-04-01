package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.TeacherSubjectRequest;
import org.example.dto.response.TeacherSubjectResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.TeacherSubjectService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TeacherSubjectController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TeacherSubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeacherSubjectService teacherSubjectService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private void mockAdminAuth() {
        User adminUser = new User("admin@uni.ru", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(jwtService.extractUsername("admin-token")).thenReturn("admin@uni.ru");
        when(jwtService.isTokenValid(eq("admin-token"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("admin@uni.ru")).thenReturn(adminUser);
    }

    private void mockStudentAuth() {
        User studentUser = new User("student@uni.ru", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(jwtService.extractUsername("student-token")).thenReturn("student@uni.ru");
        when(jwtService.isTokenValid(eq("student-token"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("student@uni.ru")).thenReturn(studentUser);
    }

    private void mockTeacherAuth() {
        User teacherUser = new User("teacher@uni.ru", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        when(jwtService.extractUsername("teacher-token")).thenReturn("teacher@uni.ru");
        when(jwtService.isTokenValid(eq("teacher-token"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("teacher@uni.ru")).thenReturn(teacherUser);
    }

    @Test
    @DisplayName("GET /api/v1/teacher-subjects — admin 200")
    void get_Admin_200() throws Exception {
        mockAdminAuth();

        TeacherSubjectResponse r = TeacherSubjectResponse.builder()
                .id(1L)
                .teacherId(10L)
                .teacherName("Иванов И.И.")
                .subjectId(20L)
                .subjectName("Алгебра")
                .build();
        when(teacherSubjectService.getAll(nullable(Long.class), eq("admin@uni.ru"))).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/teacher-subjects")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subjectName").value("Алгебра"));
    }

    @Test
    @DisplayName("GET /api/v1/teacher-subjects — teacher 200")
    void get_Teacher_200() throws Exception {
        mockTeacherAuth();

        TeacherSubjectResponse r = TeacherSubjectResponse.builder()
                .id(2L)
                .teacherId(10L)
                .teacherName("Петров П.П.")
                .subjectId(21L)
                .subjectName("Геометрия")
                .build();
        when(teacherSubjectService.getAll(nullable(Long.class), eq("teacher@uni.ru"))).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/teacher-subjects")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].teacherName").value("Петров П.П."));
    }

    @Test
    @DisplayName("GET /api/v1/teacher-subjects — student 403")
    void get_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(get("/api/v1/teacher-subjects")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/teacher-subjects — admin 201")
    void post_Admin_201() throws Exception {
        mockAdminAuth();

        TeacherSubjectRequest request = TeacherSubjectRequest.builder()
                .teacherId(10L)
                .subjectId(20L)
                .build();

        TeacherSubjectResponse created = TeacherSubjectResponse.builder()
                .id(5L)
                .teacherId(10L)
                .teacherName("Сидоров С.С.")
                .subjectId(20L)
                .subjectName("Анализ")
                .build();

        when(teacherSubjectService.create(any(TeacherSubjectRequest.class), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/teacher-subjects")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/teacher-subjects — student 403")
    void post_Student_403() throws Exception {
        mockStudentAuth();

        TeacherSubjectRequest request = TeacherSubjectRequest.builder()
                .teacherId(10L)
                .subjectId(20L)
                .build();

        mockMvc.perform(post("/api/v1/teacher-subjects")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/teacher-subjects/{id} — admin 204")
    void delete_Admin_204() throws Exception {
        mockAdminAuth();
        doNothing().when(teacherSubjectService).delete(eq(8L), anyString());

        mockMvc.perform(delete("/api/v1/teacher-subjects/8")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(teacherSubjectService).delete(eq(8L), eq("admin@uni.ru"));
    }
}
