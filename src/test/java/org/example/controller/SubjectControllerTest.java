package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.SubjectRequest;
import org.example.dto.response.SubjectResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.SubjectService;
import org.hamcrest.Matchers;
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

@WebMvcTest(controllers = SubjectController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubjectService subjectService;

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
    @DisplayName("GET /api/v1/subjects — 200")
    void getAll_200() throws Exception {
        mockAdminAuth();
        SubjectResponse s1 = SubjectResponse.builder().id(1L).name("Математика").build();
        SubjectResponse s2 = SubjectResponse.builder().id(2L).name("Физика").build();
        when(subjectService.getAll(nullable(Long.class), eq("admin@uni.ru"))).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/api/v1/subjects")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Математика"));
    }

    @Test
    @DisplayName("POST /api/v1/subjects — admin 201")
    void post_Admin_201() throws Exception {
        mockAdminAuth();

        SubjectRequest request = SubjectRequest.builder().name("Программирование").build();
        SubjectResponse created = SubjectResponse.builder().id(50L).name("Программирование").build();
        when(subjectService.create(any(SubjectRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.name").value("Программирование"));
    }

    @Test
    @DisplayName("POST /api/v1/subjects — student 403")
    void post_Student_403() throws Exception {
        mockStudentAuth();

        SubjectRequest request = SubjectRequest.builder().name("X").build();

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/subjects/{id} — admin 200")
    void put_Admin_200() throws Exception {
        mockAdminAuth();

        SubjectRequest request = SubjectRequest.builder().name("Высшая математика").build();
        SubjectResponse updated = SubjectResponse.builder().id(1L).name("Высшая математика").build();
        when(subjectService.update(eq(1L), any(SubjectRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/subjects/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Высшая математика"));
    }

    @Test
    @DisplayName("DELETE /api/v1/subjects/{id} — admin 204")
    void delete_Admin_204() throws Exception {
        mockAdminAuth();

        mockMvc.perform(delete("/api/v1/subjects/3")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(subjectService).delete(3L);
    }

    @Test
    @DisplayName("POST /api/v1/subjects — missing name 400")
    void post_MissingName_400() throws Exception {
        mockAdminAuth();

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("name")));
    }
}
