package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.GuestRegistrationLookupRequest;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.request.UpdatePendingRegistrationRequest;
import org.example.dto.response.AuthResponse;
import org.example.dto.response.GuestRegistrationStatusResponse;
import org.example.service.AuthService;
import org.example.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.submitRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/registration/status")
    public ResponseEntity<GuestRegistrationStatusResponse> registrationStatus(
            @Valid @RequestBody GuestRegistrationLookupRequest request) {
        return ResponseEntity.ok(registrationService.lookupRegistrationStatus(request));
    }

    @PutMapping("/registration/pending")
    public ResponseEntity<Void> updatePendingRegistration(
            @Valid @RequestBody UpdatePendingRegistrationRequest request) {
        registrationService.updatePendingRegistration(request);
        return ResponseEntity.ok().build();
    }
}
