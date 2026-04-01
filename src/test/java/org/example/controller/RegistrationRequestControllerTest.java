package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.ApproveRejectRequest;
import org.example.dto.response.RegistrationRequestResponse;
import org.example.exception.GlobalExceptionHandler;
import org.example.exception.ResourceNotFoundException;
import org.example.model.RegistrationStatus;
import org.example.model.UserType;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RegistrationRequestController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RegistrationRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

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
    @DisplayName("GET /api/v1/registration-requests — admin gets all requests")
    void getAllRequests_Admin_200() throws Exception {
        mockAdminAuth();

        RegistrationRequestResponse rr = RegistrationRequestResponse.builder()
                .id(1L)
                .email("new@test.ru")
                .firstName("Тест")
                .lastName("Тестов")
                .userType(UserType.STUDENT)
                .status(RegistrationStatus.PENDING)
                .universityId(1L)
                .universityName("МГУ")
                .createdAt(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();

        when(registrationService.getAllRequests(isNull(), isNull(), isNull(), eq("admin@uni.ru")))
                .thenReturn(List.of(rr));

        mockMvc.perform(get("/api/v1/registration-requests")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("new@test.ru"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/registration-requests?status=PENDING — admin gets filtered requests")
    void getAllRequests_FilterByStatus_200() throws Exception {
        mockAdminAuth();

        when(registrationService.getAllRequests(eq(RegistrationStatus.PENDING), isNull(), isNull(), eq("admin@uni.ru")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/registration-requests")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(registrationService).getAllRequests(RegistrationStatus.PENDING, null, null, "admin@uni.ru");
    }

    @Test
    @DisplayName("GET /api/v1/registration-requests — student gets 403")
    void getAllRequests_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(get("/api/v1/registration-requests")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/registration-requests — no auth gets 403")
    void getAllRequests_NoAuth_403() throws Exception {
        mockMvc.perform(get("/api/v1/registration-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/registration-requests/{id} — admin gets request by id")
    void getById_Admin_200() throws Exception {
        mockAdminAuth();

        RegistrationRequestResponse rr = RegistrationRequestResponse.builder()
                .id(5L)
                .email("user@test.ru")
                .firstName("Пользователь")
                .lastName("Тестовый")
                .userType(UserType.TEACHER)
                .status(RegistrationStatus.PENDING)
                .build();

        when(registrationService.getRequestById(eq(5L), eq("admin@uni.ru"))).thenReturn(rr);

        mockMvc.perform(get("/api/v1/registration-requests/5")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("user@test.ru"));
    }

    @Test
    @DisplayName("GET /api/v1/registration-requests/{id} — not found returns 404")
    void getById_NotFound_404() throws Exception {
        mockAdminAuth();

        when(registrationService.getRequestById(eq(99L), eq("admin@uni.ru")))
                .thenThrow(new ResourceNotFoundException("Заявка на регистрацию не найдена"));

        mockMvc.perform(get("/api/v1/registration-requests/99")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Заявка на регистрацию не найдена"));
    }

    @Test
    @DisplayName("PUT /api/v1/registration-requests/{id}/approve — admin approves")
    void approve_Admin_200() throws Exception {
        mockAdminAuth();
        doNothing().when(registrationService).approveRequest(eq(1L), eq("admin@uni.ru"));

        mockMvc.perform(put("/api/v1/registration-requests/1/approve")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        verify(registrationService).approveRequest(1L, "admin@uni.ru");
    }

    @Test
    @DisplayName("PUT /api/v1/registration-requests/{id}/reject — admin rejects with reason")
    void reject_Admin_200() throws Exception {
        mockAdminAuth();

        ApproveRejectRequest rejectBody = ApproveRejectRequest.builder()
                .rejectionReason("Некорректные данные")
                .build();

        doNothing().when(registrationService).rejectRequest(eq(1L), any(ApproveRejectRequest.class), eq("admin@uni.ru"));

        mockMvc.perform(put("/api/v1/registration-requests/1/reject")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectBody)))
                .andExpect(status().isOk());

        verify(registrationService).rejectRequest(eq(1L), any(ApproveRejectRequest.class), eq("admin@uni.ru"));
    }

    @Test
    @DisplayName("PUT /api/v1/registration-requests/{id}/approve — student gets 403")
    void approve_Student_403() throws Exception {
        mockStudentAuth();

        mockMvc.perform(put("/api/v1/registration-requests/1/approve")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());

        verify(registrationService, never()).approveRequest(anyLong(), anyString());
    }
}
