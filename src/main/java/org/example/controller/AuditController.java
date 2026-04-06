package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.AuditLogResponse;
import org.example.service.AuditService;
import org.example.service.UniversityScopeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final UniversityScopeService universityScopeService;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long universityId,
            Principal principal) {
        String email = principal.getName();
        Long restrictUni = universityScopeService.isSuperAdmin(email) ? universityId
                : universityScopeService.requireCampusUniversityId(email);
        return ResponseEntity.ok(auditService.search(userId, action, entityType, from, to, restrictUni));
    }
}
