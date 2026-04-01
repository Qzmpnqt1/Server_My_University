package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.AuthResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.GlobalExceptionHandler;
import org.example.model.UserType;
import org.example.service.AuthService;
import org.example.service.JwtService;
import org.example.service.RegistrationService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/v1/auth/login — valid credentials returns 200 with token")
    void loginReturns200WithToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@test.ru")
                .password("password")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token-123")
                .userId(1L)
                .email("test@test.ru")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("test@test.ru"))
                .andExpect(jsonPath("$.userType").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — bad credentials returns 400")
    void loginBadCredentialsReturnsError() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("bad@test.ru")
                .password("wrong")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadRequestException("Неверный пароль"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный пароль"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — missing email returns 400 validation error")
    void loginMissingEmailReturns400() throws Exception {
        String body = "{\"password\":\"password\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — invalid email format returns 400")
    void loginInvalidEmailFormatReturns400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("not-an-email")
                .password("password")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — valid student registration returns 201")
    void registerStudentReturns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.ru")
                .password("password123")
                .firstName("Пётр")
                .lastName("Петров")
                .userType(UserType.STUDENT)
                .universityId(1L)
                .groupId(1L)
                .build();

        doNothing().when(registrationService).submitRegistration(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(registrationService).submitRegistration(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — duplicate email returns 409")
    void registerDuplicateEmailReturns409() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.ru")
                .password("password123")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .universityId(1L)
                .groupId(1L)
                .build();

        doThrow(new ConflictException("Пользователь с таким email уже существует"))
                .when(registrationService).submitRegistration(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Пользователь с таким email уже существует"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — missing fields returns 400")
    void registerMissingFieldsReturns400() throws Exception {
        String body = "{\"email\":\"test@test.ru\",\"password\":\"pass12\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — short password returns 400")
    void registerShortPasswordReturns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.ru")
                .password("123")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .universityId(1L)
                .groupId(1L)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — invalid email format returns 400")
    void registerInvalidEmailReturns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("password123")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .universityId(1L)
                .groupId(1L)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
