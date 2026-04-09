package org.example.controller;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.example.config.SecurityConfig;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.JwtService;
import org.example.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Негативные сценарии JWT (фильтр до контроллера).
 */
@Tag("security")
@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class JwtSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Невалидный JWT — 401")
    void invalidJwt_returns401() throws Exception {
        when(jwtService.extractUsername(anyString())).thenThrow(new JwtException("invalid"));

        mockMvc.perform(get("/api/v1/profile/me")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Просроченный JWT — 401")
    void expiredJwt_returns401() throws Exception {
        when(jwtService.extractUsername(anyString())).thenThrow(
                new ExpiredJwtException(null, null, "expired"));

        mockMvc.perform(get("/api/v1/profile/me")
                        .header("Authorization", "Bearer expired.token.here"))
                .andExpect(status().isUnauthorized());
    }
}
