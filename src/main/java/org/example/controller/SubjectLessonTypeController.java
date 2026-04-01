package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectLessonTypeRequest;
import org.example.dto.response.SubjectLessonTypeResponse;
import org.example.service.SubjectLessonTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subject-lesson-types")
@RequiredArgsConstructor
public class SubjectLessonTypeController {

    private final SubjectLessonTypeService subjectLessonTypeService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    public ResponseEntity<List<SubjectLessonTypeResponse>> getAll(
            @RequestParam(required = false) Long subjectDirectionId,
            Principal principal) {
        return ResponseEntity.ok(subjectLessonTypeService.getAll(subjectDirectionId, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectLessonTypeResponse> create(
            @Valid @RequestBody SubjectLessonTypeRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectLessonTypeService.create(request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        subjectLessonTypeService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
