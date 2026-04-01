package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.InstituteRequest;
import org.example.dto.response.InstituteResponse;
import org.example.service.InstituteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/institutes")
@RequiredArgsConstructor
public class InstituteController {

    private final InstituteService instituteService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    public ResponseEntity<List<InstituteResponse>> getAll(
            @RequestParam(required = false) Long universityId,
            Principal principal) {
        return ResponseEntity.ok(instituteService.getAll(universityId, emailOrNull(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstituteResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(instituteService.getById(id, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InstituteResponse> create(@Valid @RequestBody InstituteRequest request,
                                                    Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(instituteService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InstituteResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody InstituteRequest request,
                                                    Principal principal) {
        return ResponseEntity.ok(instituteService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        instituteService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
