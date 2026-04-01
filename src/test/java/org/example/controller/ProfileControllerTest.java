package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.ChangeEmailRequest;
import org.example.dto.request.ChangePasswordRequest;
import org.example.dto.response.UserProfileResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.GlobalExceptionHandler;
import org.example.model.UserType;
import org.example.service.JwtService;
import org.example.service.UserProfileService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private void mockStudentAuth() {
        User springUser = new User("student@test.ru", "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(jwtService.extractUsername("student-token")).thenReturn("student@test.ru");
        when(jwtService.isTokenValid(eq("student-token"), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("student@test.ru")).thenReturn(springUser);
    }

    @Test
    @DisplayName("GET /api/v1/profile — authenticated user gets profile")
    void getMyProfile_200() throws Exception {
        mockStudentAuth();

        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .email("student@test.ru")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .isActive(true)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .studentProfile(UserProfileResponse.StudentProfileInfo.builder()
                        .groupId(1L)
                        .groupName("ИТ-101")
                        .instituteId(1L)
                        .instituteName("Институт информатики")
                        .build())
                .build();

        when(userProfileService.getMyProfile("student@test.ru")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@test.ru"))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.userType").value("STUDENT"))
                .andExpect(jsonPath("$.studentProfile.groupName").value("ИТ-101"));
    }

    @Test
    @DisplayName("GET /api/v1/profile — no auth returns 403")
    void getMyProfile_NoAuth_403() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/profile/email — change email succeeds")
    void changeEmail_200() throws Exception {
        mockStudentAuth();

        ChangeEmailRequest request = ChangeEmailRequest.builder()
                .newEmail("new@test.ru")
                .currentPassword("password123")
                .build();

        doNothing().when(userProfileService).changeEmail(eq("student@test.ru"), any(ChangeEmailRequest.class));

        mockMvc.perform(put("/api/v1/profile/email")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userProfileService).changeEmail(eq("student@test.ru"), any(ChangeEmailRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/profile/email — duplicate email returns 409")
    void changeEmail_Conflict_409() throws Exception {
        mockStudentAuth();

        ChangeEmailRequest request = ChangeEmailRequest.builder()
                .newEmail("taken@test.ru")
                .currentPassword("password123")
                .build();

        doThrow(new ConflictException("Email уже используется другим пользователем"))
                .when(userProfileService).changeEmail(eq("student@test.ru"), any(ChangeEmailRequest.class));

        mockMvc.perform(put("/api/v1/profile/email")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email уже используется другим пользователем"));
    }

    @Test
    @DisplayName("PUT /api/v1/profile/email — invalid email format returns 400")
    void changeEmail_InvalidFormat_400() throws Exception {
        mockStudentAuth();

        String body = "{\"newEmail\":\"not-email\"}";

        mockMvc.perform(put("/api/v1/profile/email")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/profile/password — change password succeeds")
    void changePassword_200() throws Exception {
        mockStudentAuth();

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword123")
                .newPasswordConfirm("newPassword123")
                .build();

        doNothing().when(userProfileService).changePassword(eq("student@test.ru"), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/profile/password")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userProfileService).changePassword(eq("student@test.ru"), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/profile/password — wrong old password returns 400")
    void changePassword_WrongOld_400() throws Exception {
        mockStudentAuth();

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrong")
                .newPassword("newPassword123")
                .build();

        doThrow(new BadRequestException("Неверный текущий пароль"))
                .when(userProfileService).changePassword(eq("student@test.ru"), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/v1/profile/password")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/profile/password — short new password returns 400")
    void changePassword_ShortNew_400() throws Exception {
        mockStudentAuth();

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("12")
                .newPasswordConfirm("12")
                .build();

        mockMvc.perform(put("/api/v1/profile/password")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
