package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.StudyDirectionRequest;
import org.example.dto.response.StudyDirectionResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.StudyDirectionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StudyDirectionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class StudyDirectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudyDirectionService studyDirectionService;

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

    @Test
    @DisplayName("GET /api/v1/directions — 200")
    void getAll_200() throws Exception {
        StudyDirectionResponse r1 = StudyDirectionResponse.builder()
                .id(1L).name("Прикладная информатика").code("09.03.03")
                .instituteId(10L).instituteName("ИИ").build();
        when(studyDirectionService.getAll(nullable(Long.class), nullable(String.class))).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/directions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("09.03.03"));
    }

    @Test
    @DisplayName("GET /api/v1/directions/{id} — 200")
    void getById_200() throws Exception {
        StudyDirectionResponse r = StudyDirectionResponse.builder()
                .id(2L).name("ИВТ").code("09.03.01")
                .instituteId(1L).instituteName("Институт").build();
        when(studyDirectionService.getById(eq(2L), nullable(String.class))).thenReturn(r);

        mockMvc.perform(get("/api/v1/directions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("ИВТ"));
    }

    @Test
    @DisplayName("POST /api/v1/directions — admin 201")
    void post_Admin_201() throws Exception {
        mockAdminAuth();

        StudyDirectionRequest request = StudyDirectionRequest.builder()
                .name("Новое направление")
                .code("01.02.03")
                .instituteId(5L)
                .build();
        StudyDirectionResponse created = StudyDirectionResponse.builder()
                .id(99L).name("Новое направление").code("01.02.03")
                .instituteId(5L).instituteName("Институт X").build();
        when(studyDirectionService.create(any(StudyDirectionRequest.class), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/directions")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.instituteName").value("Институт X"));
    }

    @Test
    @DisplayName("POST /api/v1/directions — student 403")
    void post_Student_403() throws Exception {
        mockStudentAuth();

        StudyDirectionRequest request = StudyDirectionRequest.builder()
                .name("X").code("00").instituteId(1L).build();

        mockMvc.perform(post("/api/v1/directions")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/directions/{id} — admin 200")
    void put_Admin_200() throws Exception {
        mockAdminAuth();

        StudyDirectionRequest request = StudyDirectionRequest.builder()
                .name("Обновлено")
                .code("09.03.99")
                .instituteId(1L)
                .build();
        StudyDirectionResponse updated = StudyDirectionResponse.builder()
                .id(1L).name("Обновлено").code("09.03.99")
                .instituteId(1L).instituteName("Институт").build();
        when(studyDirectionService.update(eq(1L), any(StudyDirectionRequest.class), anyString())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/directions/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Обновлено"));
    }

    @Test
    @DisplayName("DELETE /api/v1/directions/{id} — admin 204")
    void delete_Admin_204() throws Exception {
        mockAdminAuth();

        mockMvc.perform(delete("/api/v1/directions/7")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(studyDirectionService).delete(eq(7L), eq("admin@uni.ru"));
    }

    @Test
    @DisplayName("DELETE /api/v1/directions/{id} — student 403")
    void delete_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(delete("/api/v1/directions/7")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }
}
