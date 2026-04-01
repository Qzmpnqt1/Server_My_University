package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.*;
import org.example.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/me/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentPerformanceSummaryResponse> myStudentSummary(
            Principal principal,
            @RequestParam(required = false) Integer course,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(statisticsService.getMyStudentPerformanceSummary(
                principal.getName(), course, semester));
    }

    @GetMapping("/subject/{subjectDirectionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<SubjectStatisticsResponse> getSubjectStatistics(
            @PathVariable Long subjectDirectionId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getSubjectStatistics(subjectDirectionId, principal.getName()));
    }

    @GetMapping("/practices/{subjectDirectionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<PracticeStatisticsResponse> getPracticeStatistics(
            @PathVariable Long subjectDirectionId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getPracticeStatistics(subjectDirectionId, principal.getName()));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GroupStatisticsResponse> getGroupStatistics(@PathVariable Long groupId,
                                                                      Principal principal) {
        return ResponseEntity.ok(statisticsService.getGroupStatistics(groupId, principal.getName()));
    }

    @GetMapping("/direction/{directionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<DirectionStatisticsResponse> getDirectionStatistics(
            @PathVariable Long directionId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getDirectionStatistics(directionId, principal.getName()));
    }

    @GetMapping("/institute/{instituteId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InstituteStatisticsResponse> getInstituteStatistics(
            @PathVariable Long instituteId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getInstituteStatistics(instituteId, principal.getName()));
    }

    @GetMapping("/university/{universityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UniversityStatisticsResponse> getUniversityStatistics(
            @PathVariable Long universityId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getUniversityStatistics(universityId, principal.getName()));
    }

    @GetMapping("/schedule/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<ScheduleStatisticsResponse> getTeacherScheduleStatistics(
            @PathVariable Long teacherId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getTeacherScheduleStatistics(teacherId, principal.getName()));
    }

    @GetMapping("/schedule/group/{groupId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<ScheduleStatisticsResponse> getGroupScheduleStatistics(
            @PathVariable Long groupId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getGroupScheduleStatistics(groupId, principal.getName()));
    }

    @GetMapping("/schedule/classroom/{classroomId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<ScheduleStatisticsResponse> getClassroomScheduleStatistics(
            @PathVariable Long classroomId,
            Principal principal) {
        return ResponseEntity.ok(statisticsService.getClassroomScheduleStatistics(classroomId, principal.getName()));
    }
}
