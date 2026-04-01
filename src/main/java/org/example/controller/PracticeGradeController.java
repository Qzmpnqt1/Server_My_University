package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.PracticeGradeRequest;
import org.example.dto.response.PracticeGradeResponse;
import org.example.service.PracticeGradeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/practice-grades")
@RequiredArgsConstructor
public class PracticeGradeController {

    private final PracticeGradeService practiceGradeService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<PracticeGradeResponse>> getMyPracticeGrades(
            Principal principal,
            @RequestParam(required = false) Long subjectDirectionId) {
        return ResponseEntity.ok(
                practiceGradeService.getMyPracticeGrades(principal.getName(), subjectDirectionId));
    }

    @GetMapping("/by-practice/{practiceId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<PracticeGradeResponse>> getByPractice(
            @PathVariable Long practiceId,
            Principal principal) {
        return ResponseEntity.ok(practiceGradeService.getByPractice(practiceId, principal.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<PracticeGradeResponse> create(
            @Valid @RequestBody PracticeGradeRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(practiceGradeService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<PracticeGradeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PracticeGradeRequest request,
            Principal principal) {
        return ResponseEntity.ok(practiceGradeService.update(id, request, principal.getName()));
    }
}
