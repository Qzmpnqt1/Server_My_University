package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.GradeRequest;
import org.example.dto.response.GradeResponse;
import org.example.dto.response.TeacherJournalResponse;
import org.example.service.GradeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<GradeResponse>> getMyGrades(Principal principal) {
        return ResponseEntity.ok(gradeService.getMyGrades(principal.getName()));
    }

    @GetMapping("/by-student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<GradeResponse>> getByStudent(
            @PathVariable Long studentId,
            Principal principal) {
        return ResponseEntity.ok(gradeService.getByStudent(studentId, principal.getName()));
    }

    @GetMapping("/by-subject-direction/{subjectDirectionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<GradeResponse>> getBySubjectDirection(
            @PathVariable Long subjectDirectionId,
            Principal principal) {
        return ResponseEntity.ok(gradeService.getBySubjectDirection(subjectDirectionId, principal.getName()));
    }

    @GetMapping("/journal/{subjectDirectionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<TeacherJournalResponse> getJournal(
            @PathVariable Long subjectDirectionId,
            Principal principal) {
        return ResponseEntity.ok(gradeService.getTeacherJournal(subjectDirectionId, principal.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GradeResponse> create(@Valid @RequestBody GradeRequest request,
                                                Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gradeService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GradeResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody GradeRequest request,
                                                Principal principal) {
        return ResponseEntity.ok(gradeService.update(id, request, principal.getName()));
    }
}
