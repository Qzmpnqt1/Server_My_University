package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.ApproveRejectRequest;
import org.example.dto.response.RegistrationRequestResponse;
import org.example.model.RegistrationStatus;
import org.example.model.UserType;
import org.example.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/registration-requests")
@RequiredArgsConstructor
public class RegistrationRequestController {

    private final RegistrationService registrationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationRequestResponse>> getAll(
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) Long instituteId,
            Principal principal) {
        return ResponseEntity.ok(registrationService.getAllRequests(status, userType, universityId, instituteId,
                principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegistrationRequestResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(registrationService.getRequestById(id, principal.getName()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable Long id, Principal principal) {
        registrationService.approveRequest(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @Valid @RequestBody ApproveRejectRequest request,
                                       Principal principal) {
        registrationService.rejectRequest(id, request, principal.getName());
        return ResponseEntity.ok().build();
    }
}
