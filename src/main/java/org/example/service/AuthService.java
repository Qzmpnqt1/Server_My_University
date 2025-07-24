package org.example.service;

import org.example.dto.ApiResponse;
import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.dto.RegistrationRequestDTO;

public interface AuthService {
    ApiResponse<AuthResponse> login(AuthRequest request);
    ApiResponse<Void> register(RegistrationRequestDTO request);
} 