package org.example.service;

import org.example.dto.request.CreateAdminAccountRequest;
import org.example.dto.response.UserProfileResponse;
import org.example.model.UserType;

import java.util.List;

public interface UserService {
    List<UserProfileResponse> getAllUsers(UserType userType, Boolean isActive, Long universityId,
                                          Long instituteId, Long groupId, String searchQuery, String actorEmail);
    UserProfileResponse getUserById(Long id, String actorEmail);
    UserProfileResponse createAdminAccount(CreateAdminAccountRequest request, String actorEmail);
    void activateUser(Long id, String adminEmail);
    void deactivateUser(Long id, String adminEmail);
}
