package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.SubjectInDirectionRequest;
import org.example.dto.response.SubjectInDirectionResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.SubjectInDirectionService;
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

@WebMvcTest(controllers = SubjectInDirectionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SubjectInDirectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubjectInDirectionService subjectInDirectionService;

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
    @DisplayName("GET /api/v1/subjects-in-directions — 200")
    void getAll_200() throws Exception {
        mockAdminAuth();
        SubjectInDirectionResponse r = SubjectInDirectionResponse.builder()
                .id(1L)
                .subjectId(10L)
                .subjectName("Математика")
                .directionId(2L)
                .directionName("ПИ")
                .course(1)
                .semester(1)
                .build();
        when(subjectInDirectionService.getAll(nullable(Long.class), eq("admin@uni.ru"))).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/subjects-in-directions")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].subjectName").value("Математика"));
    }

    @Test
    @DisplayName("POST /api/v1/subjects-in-directions — admin 201")
    void post_Admin_201() throws Exception {
        mockAdminAuth();

        SubjectInDirectionRequest request = SubjectInDirectionRequest.builder()
                .subjectId(10L)
                .directionId(2L)
                .course(1)
                .semester(1)
                .build();

        SubjectInDirectionResponse created = SubjectInDirectionResponse.builder()
                .id(5L)
                .subjectId(10L)
                .subjectName("Физика")
                .directionId(2L)
                .directionName("ПИ")
                .course(1)
                .semester(1)
                .build();

        when(subjectInDirectionService.create(any(SubjectInDirectionRequest.class), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/subjects-in-directions")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.subjectName").value("Физика"));
    }

    @Test
    @DisplayName("POST /api/v1/subjects-in-directions — student 403")
    void post_Student_403() throws Exception {
        mockStudentAuth();

        SubjectInDirectionRequest request = SubjectInDirectionRequest.builder()
                .subjectId(10L)
                .directionId(2L)
                .course(1)
                .semester(1)
                .build();

        mockMvc.perform(post("/api/v1/subjects-in-directions")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/subjects-in-directions/{id} — admin 204")
    void delete_Admin_204() throws Exception {
        mockAdminAuth();
        doNothing().when(subjectInDirectionService).delete(eq(7L), anyString());

        mockMvc.perform(delete("/api/v1/subjects-in-directions/7")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(subjectInDirectionService).delete(eq(7L), eq("admin@uni.ru"));
    }
}
