package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.StudyDirectionRequest;
import org.example.dto.response.StudyDirectionResponse;
import org.example.service.StudyDirectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/directions")
@RequiredArgsConstructor
public class StudyDirectionController {

    private final StudyDirectionService studyDirectionService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    public ResponseEntity<List<StudyDirectionResponse>> getAll(
            @RequestParam(required = false) Long instituteId,
            @RequestParam(required = false) Long universityId,
            Principal principal) {
        return ResponseEntity.ok(studyDirectionService.getAll(instituteId, universityId, emailOrNull(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyDirectionResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(studyDirectionService.getById(id, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudyDirectionResponse> create(@Valid @RequestBody StudyDirectionRequest request,
                                                         Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studyDirectionService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudyDirectionResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody StudyDirectionRequest request,
                                                         Principal principal) {
        return ResponseEntity.ok(studyDirectionService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        studyDirectionService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
