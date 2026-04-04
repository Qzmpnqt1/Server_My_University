package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.ScheduleCompareRequest;
import org.example.dto.request.ScheduleRequest;
import org.example.dto.response.*;
import org.example.service.ScheduleAuthorizationService;
import org.example.service.ScheduleCompareService;
import org.example.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleAuthorizationService scheduleAuthorizationService;
    private final ScheduleCompareService scheduleCompareService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleResponse>> query(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer dayOfWeek,
            Principal principal) {
        String email = principal.getName();
        if (groupId != null) {
            scheduleAuthorizationService.ensureCanViewGroupSchedule(email, groupId);
            return ResponseEntity.ok(scheduleService.getByGroup(groupId, weekNumber, dayOfWeek));
        }
        if (teacherId != null) {
            scheduleAuthorizationService.ensureCanViewTeacherSchedule(email, teacherId);
            return ResponseEntity.ok(scheduleService.getByTeacher(teacherId, weekNumber, dayOfWeek));
        }
        scheduleAuthorizationService.ensureAdmin(email);
        return ResponseEntity.ok(scheduleService.getAllForAdmin(email));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    public ResponseEntity<List<ScheduleResponse>> getMySchedule(
            Principal principal,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer dayOfWeek) {
        return ResponseEntity.ok(scheduleService.getMySchedule(principal.getName(), weekNumber, dayOfWeek));
    }

    @GetMapping("/my/linked-groups")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<ScheduleResponse>> getLinkedGroupsSchedule(
            Principal principal,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer dayOfWeek) {
        return ResponseEntity.ok(
                scheduleService.getLinkedGroupsSchedule(principal.getName(), weekNumber, dayOfWeek));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleResponse>> getByGroup(
            @PathVariable Long groupId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer dayOfWeek,
            Principal principal) {
        scheduleAuthorizationService.ensureCanViewGroupSchedule(principal.getName(), groupId);
        return ResponseEntity.ok(scheduleService.getByGroup(groupId, weekNumber, dayOfWeek));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ScheduleResponse>> getByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer dayOfWeek,
            Principal principal) {
        scheduleAuthorizationService.ensureCanViewTeacherSchedule(principal.getName(), teacherId);
        return ResponseEntity.ok(scheduleService.getByTeacher(teacherId, weekNumber, dayOfWeek));
    }

    @GetMapping("/classroom/{classroomId}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<List<ScheduleResponse>> getByClassroom(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer dayOfWeek,
            Principal principal) {
        scheduleAuthorizationService.ensureCanViewClassroomSchedule(principal.getName(), classroomId);
        return ResponseEntity.ok(scheduleService.getByClassroom(classroomId, weekNumber, dayOfWeek));
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<ScheduleCompareResultResponse> compareSchedule(
            @Valid @RequestBody ScheduleCompareRequest request,
            Principal principal) {
        return ResponseEntity.ok(scheduleCompareService.compare(request, principal.getName()));
    }

    @GetMapping("/compare/institutes")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<List<ScheduleCompareInstituteOptionResponse>> compareInstitutes(Principal principal) {
        return ResponseEntity.ok(scheduleCompareService.listInstitutes(principal.getName()));
    }

    @GetMapping("/compare/directions")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<List<ScheduleCompareDirectionOptionResponse>> compareDirections(
            @RequestParam Long instituteId,
            Principal principal) {
        return ResponseEntity.ok(scheduleCompareService.listDirections(principal.getName(), instituteId));
    }

    @GetMapping("/compare/groups")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<List<ScheduleCompareGroupOptionResponse>> compareGroups(
            @RequestParam(required = false) Long instituteId,
            @RequestParam(required = false) Long directionId,
            @RequestParam(required = false) String q,
            Principal principal) {
        return ResponseEntity.ok(
                scheduleCompareService.listGroups(principal.getName(), instituteId, directionId, q));
    }

    @GetMapping("/compare/teachers")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<List<ScheduleCompareTeacherOptionResponse>> compareTeachers(
            @RequestParam(required = false) String q,
            Principal principal) {
        return ResponseEntity.ok(scheduleCompareService.listTeachers(principal.getName(), q));
    }

    @GetMapping("/compare/classrooms")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ResponseEntity<List<ScheduleCompareClassroomOptionResponse>> compareClassrooms(
            @RequestParam(required = false) String q,
            Principal principal) {
        return ResponseEntity.ok(scheduleCompareService.listClassrooms(principal.getName(), q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id, Principal principal) {
        scheduleAuthorizationService.ensureCanViewScheduleEntry(principal.getName(), id);
        return ResponseEntity.ok(scheduleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleRequest request,
                                                   Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ScheduleRequest request,
                                                   Principal principal) {
        return ResponseEntity.ok(scheduleService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        scheduleService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
