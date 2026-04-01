package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.InstituteRequest;
import org.example.dto.response.InstituteResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.InstituteService;
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

@WebMvcTest(controllers = InstituteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InstituteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InstituteService instituteService;

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
    @DisplayName("GET /api/v1/institutes — returns 200 (no auth)")
    void getAll_200_noAuth() throws Exception {
        InstituteResponse i1 = InstituteResponse.builder()
                .id(1L).name("ИИТ").shortName("ИИТ").universityId(1L).universityName("МГУ")
                .build();
        when(instituteService.getAll(nullable(Long.class), nullable(String.class))).thenReturn(List.of(i1));

        mockMvc.perform(get("/api/v1/institutes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("ИИТ"));
    }

    @Test
    @DisplayName("GET /api/v1/institutes?universityId= — filter returns 200")
    void getAll_withUniversityId_200() throws Exception {
        InstituteResponse i1 = InstituteResponse.builder()
                .id(2L).name("Физфак").shortName("ФФ").universityId(5L).universityName("МГУ")
                .build();
        when(instituteService.getAll(eq(5L), nullable(String.class))).thenReturn(List.of(i1));

        mockMvc.perform(get("/api/v1/institutes").param("universityId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].universityId").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/institutes/{id} — returns 200 (no auth)")
    void getById_200_noAuth() throws Exception {
        InstituteResponse resp = InstituteResponse.builder()
                .id(1L).name("ИИТ").shortName("ИИТ").universityId(1L).universityName("МГУ")
                .build();
        when(instituteService.getById(eq(1L), nullable(String.class))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/institutes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.universityName").value("МГУ"));
    }

    @Test
    @DisplayName("POST /api/v1/institutes — admin returns 201")
    void post_admin_201() throws Exception {
        mockAdminAuth();
        InstituteRequest request = InstituteRequest.builder()
                .name("Новый институт")
                .shortName("НИ")
                .universityId(1L)
                .build();
        InstituteResponse created = InstituteResponse.builder()
                .id(20L)
                .name("Новый институт")
                .shortName("НИ")
                .universityId(1L)
                .universityName("МГУ")
                .build();
        when(instituteService.create(any(InstituteRequest.class), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/institutes")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("Новый институт"));
    }

    @Test
    @DisplayName("POST /api/v1/institutes — student returns 403")
    void post_student_403() throws Exception {
        mockStudentAuth();
        InstituteRequest request = InstituteRequest.builder()
                .name("X").shortName("Y").universityId(1L)
                .build();

        mockMvc.perform(post("/api/v1/institutes")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/institutes/{id} — admin returns 200")
    void put_admin_200() throws Exception {
        mockAdminAuth();
        InstituteRequest request = InstituteRequest.builder()
                .name("Обновлённый институт")
                .shortName("ОИ")
                .universityId(1L)
                .build();
        InstituteResponse updated = InstituteResponse.builder()
                .id(1L)
                .name("Обновлённый институт")
                .shortName("ОИ")
                .universityId(1L)
                .universityName("МГУ")
                .build();
        when(instituteService.update(eq(1L), any(InstituteRequest.class), anyString())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/institutes/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Обновлённый институт"));
    }

    @Test
    @DisplayName("DELETE /api/v1/institutes/{id} — admin returns 204")
    void delete_admin_204() throws Exception {
        mockAdminAuth();

        mockMvc.perform(delete("/api/v1/institutes/3")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(instituteService).delete(eq(3L), eq("admin@uni.ru"));
    }

    @Test
    @DisplayName("DELETE /api/v1/institutes/{id} — student returns 403")
    void delete_student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(delete("/api/v1/institutes/3")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }
}
