package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectInDirectionRequest;
import org.example.dto.response.SubjectInDirectionResponse;
import org.example.service.SubjectInDirectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects-in-directions")
@RequiredArgsConstructor
public class SubjectInDirectionController {

    private final SubjectInDirectionService subjectInDirectionService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    public ResponseEntity<List<SubjectInDirectionResponse>> getAll(
            @RequestParam(required = false) Long directionId,
            @RequestParam(required = false) Long universityId,
            Principal principal) {
        return ResponseEntity.ok(subjectInDirectionService.getAll(directionId, universityId, emailOrNull(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectInDirectionResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(subjectInDirectionService.getById(id, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectInDirectionResponse> create(
            @Valid @RequestBody SubjectInDirectionRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectInDirectionService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectInDirectionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubjectInDirectionRequest request,
            Principal principal) {
        return ResponseEntity.ok(subjectInDirectionService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        subjectInDirectionService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
