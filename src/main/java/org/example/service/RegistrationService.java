package org.example.service;

import org.example.dto.ApiResponse;
import org.example.dto.RegistrationRequestDTO;

public interface RegistrationService {
    /**
     * Submits a new registration request
     * 
     * @param request Registration request data
     * @return API response with the registration result
     */
    ApiResponse<?> submitRegistrationRequest(RegistrationRequestDTO request);
} 