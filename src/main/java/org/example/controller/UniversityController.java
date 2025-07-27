package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.*;
import org.example.service.UniversityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
@RequiredArgsConstructor
public class UniversityController {
    private final UniversityService universityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UniversityDTO>>> getAllUniversities() {
        List<UniversityDTO> universities = universityService.getAllUniversities();
        return ResponseEntity.ok(ApiResponse.success(universities));
    }

    @GetMapping("/{universityId}/institutes")
    public ResponseEntity<ApiResponse<List<InstituteDTO>>> getInstitutesByUniversity(
            @PathVariable Integer universityId) {
        List<InstituteDTO> institutes = universityService.getInstitutesByUniversity(universityId);
        return ResponseEntity.ok(ApiResponse.success(institutes));
    }

    @GetMapping("/institutes/{instituteId}/directions")
    public ResponseEntity<ApiResponse<List<StudyDirectionDTO>>> getDirectionsByInstitute(
            @PathVariable Integer instituteId) {
        List<StudyDirectionDTO> directions = universityService.getDirectionsByInstitute(instituteId);
        return ResponseEntity.ok(ApiResponse.success(directions));
    }

    @GetMapping("/directions/{directionId}/groups")
    public ResponseEntity<ApiResponse<List<AcademicGroupDTO>>> getGroupsByDirection(
            @PathVariable Integer directionId) {
        List<AcademicGroupDTO> groups = universityService.getGroupsByDirection(directionId);
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/directions/{directionId}/course/{course}/groups")
    public ResponseEntity<ApiResponse<List<AcademicGroupDTO>>> getGroupsByDirectionAndCourse(
            @PathVariable Integer directionId,
            @PathVariable Integer course) {
        List<AcademicGroupDTO> groups = universityService.getGroupsByDirectionAndCourse(directionId, course);
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectDTO>>> getAllSubjects() {
        List<SubjectDTO> subjects = universityService.getAllSubjects();
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping("/subjects/search")
    public ResponseEntity<ApiResponse<List<SubjectDTO>>> searchSubjects(@RequestParam String query) {
        List<SubjectDTO> subjects = universityService.searchSubjectsByName(query);
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }
    
    @GetMapping("/{universityId}/subjects")
    public ResponseEntity<ApiResponse<List<SubjectDTO>>> getSubjectsByUniversity(
            @PathVariable Integer universityId) {
        List<SubjectDTO> subjects = universityService.getSubjectsByUniversity(universityId);
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }
} 