package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.ChangeEmailRequest;
import org.example.dto.request.ChangePasswordRequest;
import org.example.dto.request.UpdatePersonalProfileRequest;
import org.example.dto.response.UserProfileResponse;
import org.example.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile(Principal principal) {
        return ResponseEntity.ok(userProfileService.getMyProfile(principal.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(Principal principal) {
        return ResponseEntity.ok(userProfileService.getMyProfile(principal.getName()));
    }

    @PutMapping
    public ResponseEntity<Void> updatePersonal(Principal principal,
                                               @Valid @RequestBody UpdatePersonalProfileRequest request) {
        userProfileService.updatePersonalProfile(principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/email")
    public ResponseEntity<Void> changeEmail(Principal principal,
                                            @Valid @RequestBody ChangeEmailRequest request) {
        userProfileService.changeEmail(principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Principal principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(principal.getName(), request);
        return ResponseEntity.ok().build();
    }
}
