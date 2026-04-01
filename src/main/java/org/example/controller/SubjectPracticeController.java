package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectPracticeRequest;
import org.example.dto.response.SubjectPracticeResponse;
import org.example.service.SubjectPracticeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subject-practices")
@RequiredArgsConstructor
public class SubjectPracticeController {

    private final SubjectPracticeService subjectPracticeService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    public ResponseEntity<List<SubjectPracticeResponse>> getBySubjectDirection(
            @RequestParam Long subjectDirectionId,
            Principal principal) {
        return ResponseEntity.ok(subjectPracticeService.getBySubjectDirection(subjectDirectionId,
                emailOrNull(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectPracticeResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(subjectPracticeService.getById(id, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectPracticeResponse> create(
            @Valid @RequestBody SubjectPracticeRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectPracticeService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectPracticeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubjectPracticeRequest request,
            Principal principal) {
        return ResponseEntity.ok(subjectPracticeService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        subjectPracticeService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
