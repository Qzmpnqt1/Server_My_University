package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.SubjectInDirectionResponse;
import org.example.dto.response.TeacherGradingPickResponse;
import org.example.dto.response.TeacherStudentAssessmentResponse;
import org.example.service.TeacherGradingCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/grades/teacher-catalog")
@RequiredArgsConstructor
public class TeacherGradingCatalogController {

    private final TeacherGradingCatalogService teacherGradingCatalogService;

    private static String email(Principal p) {
        return p != null ? p.getName() : null;
    }

    @GetMapping("/institutes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherGradingPickResponse>> institutes(Principal principal) {
        return ResponseEntity.ok(teacherGradingCatalogService.listInstitutes(email(principal)));
    }

    @GetMapping("/directions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherGradingPickResponse>> directions(
            @RequestParam Long instituteId,
            Principal principal) {
        return ResponseEntity.ok(teacherGradingCatalogService.listDirections(email(principal), instituteId));
    }

    @GetMapping("/subject-directions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SubjectInDirectionResponse>> subjectDirections(
            @RequestParam Long directionId,
            Principal principal) {
        return ResponseEntity.ok(teacherGradingCatalogService.listSubjectDirections(email(principal), directionId));
    }

    @GetMapping("/groups")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherGradingPickResponse>> groups(
            @RequestParam Long subjectDirectionId,
            Principal principal) {
        return ResponseEntity.ok(teacherGradingCatalogService.listGroups(email(principal), subjectDirectionId));
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherGradingPickResponse>> students(
            @RequestParam Long subjectDirectionId,
            @RequestParam Long groupId,
            Principal principal) {
        return ResponseEntity.ok(teacherGradingCatalogService.listStudents(email(principal), subjectDirectionId, groupId));
    }

    @GetMapping("/assessment")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherStudentAssessmentResponse> assessment(
            @RequestParam Long subjectDirectionId,
            @RequestParam Long groupId,
            @RequestParam Long studentUserId,
            Principal principal) {
        return ResponseEntity.ok(teacherGradingCatalogService.getAssessment(
                email(principal), subjectDirectionId, groupId, studentUserId));
    }
}
