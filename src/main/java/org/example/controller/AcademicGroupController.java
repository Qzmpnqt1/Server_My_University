package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.AcademicGroupRequest;
import org.example.dto.response.AcademicGroupResponse;
import org.example.service.AcademicGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class AcademicGroupController {

    private final AcademicGroupService academicGroupService;

    private static String emailOrNull(Principal principal) {
        if (principal == null) return null;
        String n = principal.getName();
        return (n == null || n.isBlank() || "anonymousUser".equals(n)) ? null : n;
    }

    @GetMapping
    public ResponseEntity<List<AcademicGroupResponse>> getAll(
            @RequestParam(required = false) Long directionId,
            Principal principal) {
        return ResponseEntity.ok(academicGroupService.getAll(directionId, emailOrNull(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicGroupResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(academicGroupService.getById(id, emailOrNull(principal)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicGroupResponse> create(@Valid @RequestBody AcademicGroupRequest request,
                                                        Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicGroupService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademicGroupResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody AcademicGroupRequest request,
                                                        Principal principal) {
        return ResponseEntity.ok(academicGroupService.update(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        academicGroupService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
