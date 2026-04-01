package org.example.service;

import org.example.dto.request.LoginRequest;
import org.example.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
