package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.UniversityRequest;
import org.example.dto.response.UniversityResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.exception.ResourceNotFoundException;
import org.example.service.JwtService;
import org.example.service.UniversityService;
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

import static org.hamcrest.Matchers.containsString;
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

@WebMvcTest(controllers = UniversityController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UniversityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UniversityService universityService;

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
    @DisplayName("GET /api/v1/universities — returns 200 with list (no auth)")
    void getAll_200_noAuth() throws Exception {
        UniversityResponse u1 = UniversityResponse.builder()
                .id(1L).name("МГУ").shortName("МГУ").city("Москва")
                .build();
        UniversityResponse u2 = UniversityResponse.builder()
                .id(2L).name("СПбГУ").shortName("СПбГУ").city("Санкт-Петербург")
                .build();
        when(universityService.getAll(nullable(String.class))).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/v1/universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("МГУ"))
                .andExpect(jsonPath("$[1].name").value("СПбГУ"));
    }

    @Test
    @DisplayName("GET /api/v1/universities/{id} — returns 200 (no auth)")
    void getById_200_noAuth() throws Exception {
        UniversityResponse resp = UniversityResponse.builder()
                .id(1L).name("МГУ").shortName("МГУ").city("Москва")
                .build();
        when(universityService.getById(eq(1L), nullable(String.class))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/universities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("МГУ"));
    }

    @Test
    @DisplayName("GET /api/v1/universities/{id} — not found returns 404")
    void getById_notFound_404() throws Exception {
        when(universityService.getById(eq(99L), nullable(String.class)))
                .thenThrow(new ResourceNotFoundException("Университет не найден с id: 99"));

        mockMvc.perform(get("/api/v1/universities/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Университет не найден с id: 99"));
    }

    @Test
    @DisplayName("POST /api/v1/universities — admin returns 201")
    void post_admin_201() throws Exception {
        mockAdminAuth();
        UniversityRequest request = UniversityRequest.builder()
                .name("Новый университет")
                .shortName("НУ")
                .city("Казань")
                .build();
        UniversityResponse created = UniversityResponse.builder()
                .id(10L)
                .name("Новый университет")
                .shortName("НУ")
                .city("Казань")
                .build();
        when(universityService.create(any(UniversityRequest.class), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/universities")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Новый университет"));
    }

    @Test
    @DisplayName("POST /api/v1/universities — student returns 403")
    void post_student_403() throws Exception {
        mockStudentAuth();
        UniversityRequest request = UniversityRequest.builder()
                .name("X").shortName("Y").city("Z")
                .build();

        mockMvc.perform(post("/api/v1/universities")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/universities — without auth returns 403")
    void post_noAuth_403() throws Exception {
        UniversityRequest request = UniversityRequest.builder()
                .name("X").shortName("Y").city("Z")
                .build();

        mockMvc.perform(post("/api/v1/universities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/universities/{id} — admin returns 200")
    void put_admin_200() throws Exception {
        mockAdminAuth();
        UniversityRequest request = UniversityRequest.builder()
                .name("Обновлённое имя")
                .shortName("ОИ")
                .city("Москва")
                .build();
        UniversityResponse updated = UniversityResponse.builder()
                .id(1L)
                .name("Обновлённое имя")
                .shortName("ОИ")
                .city("Москва")
                .build();
        when(universityService.update(eq(1L), any(UniversityRequest.class), anyString())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/universities/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Обновлённое имя"));
    }

    @Test
    @DisplayName("DELETE /api/v1/universities/{id} — admin returns 204")
    void delete_admin_204() throws Exception {
        mockAdminAuth();

        mockMvc.perform(delete("/api/v1/universities/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(universityService).delete(eq(1L), eq("admin@uni.ru"));
    }

    @Test
    @DisplayName("DELETE /api/v1/universities/{id} — student returns 403")
    void delete_student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(delete("/api/v1/universities/1")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/universities — missing name returns 400")
    void post_missingName_400() throws Exception {
        mockAdminAuth();
        String body = "{\"shortName\":\"Аббр\",\"city\":\"Город\"}";

        mockMvc.perform(post("/api/v1/universities")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("name")));
    }
}
