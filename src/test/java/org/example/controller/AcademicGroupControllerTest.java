package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.AcademicGroupRequest;
import org.example.dto.response.AcademicGroupResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.AcademicGroupService;
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

@WebMvcTest(controllers = AcademicGroupController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AcademicGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AcademicGroupService academicGroupService;

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
    @DisplayName("GET /api/v1/groups — 200")
    void getAll_200() throws Exception {
        AcademicGroupResponse g = AcademicGroupResponse.builder()
                .id(1L).name("ИТ-101").course(1).yearOfAdmission(2024)
                .directionId(2L).directionName("ИВТ").build();
        when(academicGroupService.getAll(nullable(Long.class), nullable(String.class))).thenReturn(List.of(g));

        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("ИТ-101"));
    }

    @Test
    @DisplayName("GET /api/v1/groups?directionId= — 200")
    void getAll_WithDirectionFilter_200() throws Exception {
        AcademicGroupResponse g = AcademicGroupResponse.builder()
                .id(3L).name("ИТ-201").course(2).yearOfAdmission(2023)
                .directionId(5L).directionName("ПИ").build();
        when(academicGroupService.getAll(eq(5L), nullable(String.class))).thenReturn(List.of(g));

        mockMvc.perform(get("/api/v1/groups").param("directionId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].directionId").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/groups — admin 201")
    void post_Admin_201() throws Exception {
        mockAdminAuth();

        AcademicGroupRequest request = AcademicGroupRequest.builder()
                .name("Новая группа")
                .course(1)
                .yearOfAdmission(2025)
                .directionId(2L)
                .build();
        AcademicGroupResponse created = AcademicGroupResponse.builder()
                .id(10L).name("Новая группа").course(1).yearOfAdmission(2025)
                .directionId(2L).directionName("ИВТ").build();
        when(academicGroupService.create(any(AcademicGroupRequest.class), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.directionName").value("ИВТ"));
    }

    @Test
    @DisplayName("POST /api/v1/groups — student 403")
    void post_Student_403() throws Exception {
        mockStudentAuth();

        AcademicGroupRequest request = AcademicGroupRequest.builder()
                .name("G").course(1).yearOfAdmission(2024).directionId(1L).build();

        mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/groups/{id} — admin 200")
    void put_Admin_200() throws Exception {
        mockAdminAuth();

        AcademicGroupRequest request = AcademicGroupRequest.builder()
                .name("ИТ-102")
                .course(1)
                .yearOfAdmission(2024)
                .directionId(1L)
                .build();
        AcademicGroupResponse updated = AcademicGroupResponse.builder()
                .id(1L).name("ИТ-102").course(1).yearOfAdmission(2024)
                .directionId(1L).directionName("ИВТ").build();
        when(academicGroupService.update(eq(1L), any(AcademicGroupRequest.class), anyString())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/groups/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ИТ-102"));
    }

    @Test
    @DisplayName("DELETE /api/v1/groups/{id} — admin 204")
    void delete_Admin_204() throws Exception {
        mockAdminAuth();

        mockMvc.perform(delete("/api/v1/groups/4")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(academicGroupService).delete(eq(4L), anyString());
    }
}
