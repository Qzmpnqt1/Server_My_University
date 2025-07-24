package org.example.service;

import org.example.model.Users;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(Users user);
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
} 