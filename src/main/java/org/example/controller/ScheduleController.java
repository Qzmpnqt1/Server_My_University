package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.ScheduleRequest;
import org.example.dto.response.ScheduleResponse;
import org.example.service.ScheduleAuthorizationService;
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
