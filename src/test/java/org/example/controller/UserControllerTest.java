package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.response.UserProfileResponse;
import org.example.exception.BadRequestException;
import org.example.exception.GlobalExceptionHandler;
import org.example.exception.ResourceNotFoundException;
import org.example.model.UserType;
import org.example.service.JwtService;
import org.example.service.UserService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

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
    @DisplayName("GET /api/v1/users — admin gets list of users")
    void getAll_Admin_200() throws Exception {
        mockAdminAuth();

        UserProfileResponse u1 = UserProfileResponse.builder()
                .id(1L).email("user1@test.ru").firstName("Иван").lastName("Иванов")
                .userType(UserType.STUDENT).isActive(true)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
        UserProfileResponse u2 = UserProfileResponse.builder()
                .id(2L).email("user2@test.ru").firstName("Пётр").lastName("Петров")
                .userType(UserType.TEACHER).isActive(false)
                .createdAt(LocalDateTime.of(2025, 2, 1, 0, 0))
                .build();

        when(userService.getAllUsers(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("admin@uni.ru")))
                .thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("user1@test.ru"))
                .andExpect(jsonPath("$[1].email").value("user2@test.ru"));
    }

    @Test
    @DisplayName("GET /api/v1/users — student gets 403")
    void getAll_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users — no auth gets 403")
    void getAll_NoAuth_403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} — admin gets user by id with profile")
    void getById_Admin_200() throws Exception {
        mockAdminAuth();

        UserProfileResponse resp = UserProfileResponse.builder()
                .id(1L).email("student@test.ru").firstName("Анна").lastName("Смирнова")
                .userType(UserType.STUDENT).isActive(true)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .studentProfile(UserProfileResponse.StudentProfileInfo.builder()
                        .groupId(1L).groupName("ИТ-101")
                        .instituteId(1L).instituteName("Институт информатики")
                        .build())
                .build();

        when(userService.getUserById(eq(1L), eq("admin@uni.ru"))).thenReturn(resp);

        mockMvc.perform(get("/api/v1/users/1")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@test.ru"))
                .andExpect(jsonPath("$.studentProfile.groupName").value("ИТ-101"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} — not found returns 404")
    void getById_NotFound_404() throws Exception {
        mockAdminAuth();

        when(userService.getUserById(eq(99L), eq("admin@uni.ru")))
                .thenThrow(new ResourceNotFoundException("Пользователь не найден с id: 99"));

        mockMvc.perform(get("/api/v1/users/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Пользователь не найден с id: 99"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/activate — admin activates user")
    void activate_Admin_200() throws Exception {
        mockAdminAuth();
        doNothing().when(userService).activateUser(eq(5L), eq("admin@uni.ru"));

        mockMvc.perform(put("/api/v1/users/5/activate")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        verify(userService).activateUser(5L, "admin@uni.ru");
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/activate — already active returns 400")
    void activate_AlreadyActive_400() throws Exception {
        mockAdminAuth();
        doThrow(new BadRequestException("Пользователь уже активен"))
                .when(userService).activateUser(eq(5L), eq("admin@uni.ru"));

        mockMvc.perform(put("/api/v1/users/5/activate")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Пользователь уже активен"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/deactivate — admin deactivates user")
    void deactivate_Admin_200() throws Exception {
        mockAdminAuth();
        doNothing().when(userService).deactivateUser(eq(5L), eq("admin@uni.ru"));

        mockMvc.perform(put("/api/v1/users/5/deactivate")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        verify(userService).deactivateUser(5L, "admin@uni.ru");
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/deactivate — self-deactivation returns 400")
    void deactivate_Self_400() throws Exception {
        mockAdminAuth();
        doThrow(new BadRequestException("Нельзя деактивировать собственный аккаунт"))
                .when(userService).deactivateUser(eq(1L), eq("admin@uni.ru"));

        mockMvc.perform(put("/api/v1/users/1/deactivate")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Нельзя деактивировать собственный аккаунт"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/activate — student gets 403")
    void activate_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(put("/api/v1/users/1/activate")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());

        verify(userService, never()).activateUser(anyLong(), anyString());
    }
}
