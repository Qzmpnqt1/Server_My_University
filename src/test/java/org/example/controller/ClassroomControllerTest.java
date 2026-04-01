package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.ClassroomRequest;
import org.example.dto.response.ClassroomResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.ClassroomService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClassroomController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ClassroomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClassroomService classroomService;

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
    @DisplayName("GET /api/v1/classrooms — 200")
    void getAll_200() throws Exception {
        mockAdminAuth();
        ClassroomResponse r = ClassroomResponse.builder()
                .id(1L)
                .building("Корпус А")
                .roomNumber("101")
                .capacity(30)
                .universityId(1L)
                .build();
        when(classroomService.getAll(nullable(Long.class), eq("admin@uni.ru"))).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/classrooms")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].building").value("Корпус А"));
    }

    @Test
    @DisplayName("GET /api/v1/classrooms/{id} — 200")
    void getById_200() throws Exception {
        mockAdminAuth();
        ClassroomResponse r = ClassroomResponse.builder()
                .id(2L)
                .building("Корпус Б")
                .roomNumber("205")
                .capacity(50)
                .universityId(1L)
                .build();
        when(classroomService.getById(eq(2L), eq("admin@uni.ru"))).thenReturn(r);

        mockMvc.perform(get("/api/v1/classrooms/2")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.roomNumber").value("205"));
    }

    @Test
    @DisplayName("POST /api/v1/classrooms — admin 201")
    void post_Admin_201() throws Exception {
        mockAdminAuth();

        ClassroomRequest request = ClassroomRequest.builder()
                .building("Корпус В")
                .roomNumber("310")
                .capacity(25)
                .universityId(1L)
                .build();

        ClassroomResponse created = ClassroomResponse.builder()
                .id(9L)
                .building("Корпус В")
                .roomNumber("310")
                .capacity(25)
                .universityId(1L)
                .build();

        when(classroomService.create(any(ClassroomRequest.class), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/classrooms")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    @DisplayName("POST /api/v1/classrooms — student 403")
    void post_Student_403() throws Exception {
        mockStudentAuth();

        ClassroomRequest request = ClassroomRequest.builder()
                .building("Корпус В")
                .roomNumber("310")
                .capacity(25)
                .universityId(1L)
                .build();

        mockMvc.perform(post("/api/v1/classrooms")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/classrooms/{id} — admin 200")
    void put_Admin_200() throws Exception {
        mockAdminAuth();

        ClassroomRequest request = ClassroomRequest.builder()
                .building("Корпус А")
                .roomNumber("101")
                .capacity(40)
                .universityId(1L)
                .build();

        ClassroomResponse updated = ClassroomResponse.builder()
                .id(1L)
                .building("Корпус А")
                .roomNumber("101")
                .capacity(40)
                .universityId(1L)
                .build();

        when(classroomService.update(eq(1L), any(ClassroomRequest.class), anyString())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/classrooms/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(40));
    }

    @Test
    @DisplayName("DELETE /api/v1/classrooms/{id} — admin 204")
    void delete_Admin_204() throws Exception {
        mockAdminAuth();
        doNothing().when(classroomService).delete(eq(3L), anyString());

        mockMvc.perform(delete("/api/v1/classrooms/3")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNoContent());

        verify(classroomService).delete(eq(3L), eq("admin@uni.ru"));
    }
}
