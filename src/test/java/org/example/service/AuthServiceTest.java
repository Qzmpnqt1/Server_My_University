package org.example.service;

import org.example.dto.request.LoginRequest;
import org.example.dto.response.AuthResponse;
import org.example.exception.BadRequestException;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.UsersRepository;
import org.example.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Users testUser;

    @BeforeEach
    void setUp() {
        testUser = Users.builder()
                .id(1L)
                .email("test@test.ru")
                .passwordHash("$2a$10$encoded")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Login with valid credentials returns token")
    void loginSuccess() {
        when(usersRepository.findByEmail("test@test.ru")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "$2a$10$encoded")).thenReturn(true);
        when(jwtService.generateToken(testUser)).thenReturn("jwt-token");

        LoginRequest request = LoginRequest.builder().email("test@test.ru").password("password").build();
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("test@test.ru", response.getEmail());
        assertEquals(UserType.STUDENT, response.getUserType());
    }

    @Test
    @DisplayName("Login with non-existent email throws exception")
    void loginUserNotFound() {
        when(usersRepository.findByEmail("unknown@test.ru")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder().email("unknown@test.ru").password("password").build();
        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login with wrong password throws exception")
    void loginWrongPassword() {
        when(usersRepository.findByEmail("test@test.ru")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

        LoginRequest request = LoginRequest.builder().email("test@test.ru").password("wrong").build();
        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login with inactive user throws exception")
    void loginInactiveUser() {
        testUser.setIsActive(false);
        when(usersRepository.findByEmail("test@test.ru")).thenReturn(Optional.of(testUser));

        LoginRequest request = LoginRequest.builder().email("test@test.ru").password("password").build();
        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Password is properly hashed with BCrypt")
    void passwordHashingVerification() {
        when(usersRepository.findByEmail("test@test.ru")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "$2a$10$encoded")).thenReturn(true);
        when(jwtService.generateToken(testUser)).thenReturn("token");

        LoginRequest request = LoginRequest.builder().email("test@test.ru").password("password").build();
        authService.login(request);

        verify(passwordEncoder).matches("password", "$2a$10$encoded");
        verify(passwordEncoder, never()).encode(anyString());
    }
}
