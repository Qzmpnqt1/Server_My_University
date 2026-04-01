package org.example.service;

import org.example.dto.request.ChangeEmailRequest;
import org.example.dto.request.ChangePasswordRequest;
import org.example.dto.request.UpdatePersonalProfileRequest;
import org.example.dto.response.UserProfileResponse;
import org.springframework.transaction.annotation.Transactional;

public interface UserProfileService {
    @Transactional(readOnly = true)
    UserProfileResponse getMyProfile(String email);

    void updatePersonalProfile(String email, UpdatePersonalProfileRequest request);

    void changeEmail(String currentEmail, ChangeEmailRequest request);

    void changePassword(String currentEmail, ChangePasswordRequest request);
}
