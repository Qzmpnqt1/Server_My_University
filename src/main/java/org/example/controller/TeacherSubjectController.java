package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.TeacherSubjectReplaceRequest;
import org.example.dto.request.TeacherSubjectRequest;
import org.example.dto.response.TeacherSubjectResponse;
import org.example.service.TeacherSubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-subjects")
@RequiredArgsConstructor
public class TeacherSubjectController {

    private final TeacherSubjectService teacherSubjectService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<TeacherSubjectResponse>> getAll(
            @RequestParam(required = false) Long teacherId,
            Principal principal) {
        return ResponseEntity.ok(teacherSubjectService.getAll(teacherId, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherSubjectResponse> create(
            @Valid @RequestBody TeacherSubjectRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherSubjectService.create(request, principal.getName()));
    }

    @PutMapping("/teachers/{teacherProfileId}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeacherSubjectResponse>> replaceAssignments(
            @PathVariable Long teacherProfileId,
            @Valid @RequestBody TeacherSubjectReplaceRequest request,
            Principal principal) {
        return ResponseEntity.ok(teacherSubjectService.replaceAssignments(
                teacherProfileId, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        teacherSubjectService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
