package org.example.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.service.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final String JWT_SECRET =
            "9a4f2c8d3b7a1e6f45c8a0b3f267d8b1d4e6f3c8a9d2b5f8e3a9c8b5f6a8a3d9";

    private JwtServiceImpl jwtService;
    private Users testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

        testUser = Users.builder()
                .id(42L)
                .email("test@test.ru")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .userType(UserType.STUDENT)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Generated token subject matches user email")
    void generateToken_ContainsCorrectSubject() {
        String token = jwtService.generateToken(testUser);

        assertEquals("test@test.ru", jwtService.extractUsername(token));
    }

    @Test
    @DisplayName("Generated token carries user id claim")
    void generateToken_ContainsUserId() {
        String token = jwtService.generateToken(testUser);

        assertEquals(testUser.getId(), jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Generated token carries STUDENT user type")
    void generateToken_ContainsUserType() {
        String token = jwtService.generateToken(testUser);

        assertEquals("STUDENT", jwtService.extractUserType(token));
    }

    @Test
    @DisplayName("Token is valid when username matches UserDetails")
    void isTokenValid_ValidToken() {
        String token = jwtService.generateToken(testUser);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test@test.ru");

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Token is invalid when UserDetails username differs")
    void isTokenValid_WrongUser() {
        String token = jwtService.generateToken(testUser);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("other@test.ru");

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Expired token causes ExpiredJwtException when validating")
    void isTokenValid_ExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expiredToken = jwtService.generateToken(testUser);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test@test.ru");

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    @DisplayName("Garbage token string fails JWT parsing")
    void extractUsername_InvalidToken() {
        assertThrows(JwtException.class, () -> jwtService.extractUsername("invalid-token"));
    }
}
