package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.ChangeEmailRequest;
import org.example.dto.request.ChangePasswordRequest;
import org.example.dto.request.UpdatePersonalProfileRequest;
import org.example.dto.response.UserProfileResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.UserProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UsersRepository usersRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getMyProfile(String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        UserProfileResponse.UserProfileResponseBuilder responseBuilder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .userType(user.getUserType())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt());

        switch (user.getUserType()) {
            case STUDENT -> {
                StudentProfile profile = studentProfileRepository.findFetchedByUserId(user.getId())
                        .orElse(null);
                if (profile != null) {
                    responseBuilder.studentProfile(UserProfileResponse.StudentProfileInfo.builder()
                            .groupId(profile.getGroup().getId())
                            .groupName(profile.getGroup().getName())
                            .instituteId(profile.getInstitute().getId())
                            .instituteName(profile.getInstitute().getName())
                            .build());
                }
            }
            case TEACHER -> {
                TeacherProfile profile = teacherProfileRepository.findFetchedByUserId(user.getId())
                        .orElse(null);
                if (profile != null) {
                    responseBuilder.teacherProfile(UserProfileResponse.TeacherProfileInfo.builder()
                            .instituteId(profile.getInstitute() != null ? profile.getInstitute().getId() : null)
                            .instituteName(profile.getInstitute() != null ? profile.getInstitute().getName() : null)
                            .position(profile.getPosition())
                            .build());
                }
            }
            case ADMIN -> {
                AdminProfile profile = adminProfileRepository.findFetchedByUserId(user.getId())
                        .orElse(null);
                if (profile != null) {
                    responseBuilder.adminProfile(UserProfileResponse.AdminProfileInfo.builder()
                            .universityId(profile.getUniversity() != null ? profile.getUniversity().getId() : null)
                            .universityName(profile.getUniversity() != null ? profile.getUniversity().getName() : null)
                            .role("ADMIN")
                            .build());
                }
            }
        }

        return responseBuilder.build();
    }

    @Override
    @Transactional
    public void updatePersonalProfile(String email, UpdatePersonalProfileRequest request) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMiddleName(request.getMiddleName());
        usersRepository.save(user);
    }

    @Override
    @Transactional
    public void changeEmail(String currentEmail, ChangeEmailRequest request) {
        Users user = usersRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный пароль");
        }

        if (usersRepository.existsByEmail(request.getNewEmail())) {
            throw new ConflictException("Email уже используется другим пользователем");
        }

        user.setEmail(request.getNewEmail());
        usersRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String currentEmail, ChangePasswordRequest request) {
        Users user = usersRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Неверный текущий пароль");
        }
        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new BadRequestException("Новый пароль и подтверждение не совпадают");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        usersRepository.save(user);
    }
}
