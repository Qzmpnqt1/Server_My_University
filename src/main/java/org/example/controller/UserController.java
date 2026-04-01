package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponse;
import org.example.model.UserType;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAll(
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long universityId,
            @RequestParam(required = false) Long instituteId,
            @RequestParam(required = false) Long groupId,
            Principal principal) {
        return ResponseEntity.ok(userService.getAllUsers(userType, isActive, universityId, instituteId, groupId,
                principal.getName()));
    }

    /** Только числовой id, иначе перехватываются пути вроде /chat-contacts */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(userService.getUserById(id, principal.getName()));
    }

    @PutMapping("/{id:\\d+}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activate(@PathVariable Long id, Principal principal) {
        userService.activateUser(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id:\\d+}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, Principal principal) {
        userService.deactivateUser(id, principal.getName());
        return ResponseEntity.ok().build();
    }
}
