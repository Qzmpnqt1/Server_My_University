package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.UniversityRequest;
import org.example.dto.response.UniversityResponse;
import org.example.service.UniversityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    public ResponseEntity<List<UniversityResponse>> getAll(Principal principal) {
        return ResponseEntity.ok(universityService.getAll(emailOrNull(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UniversityResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(universityService.getById(id, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UniversityResponse> create(@Valid @RequestBody UniversityRequest request,
                                                     Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(universityService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UniversityResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UniversityRequest request,
                                                     Principal principal) {
        return ResponseEntity.ok(universityService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        universityService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
