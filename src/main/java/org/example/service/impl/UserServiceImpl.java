package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.AuditService;
import org.example.service.UniversityScopeService;
import org.example.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UsersRepository usersRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final AuditService auditService;
    private final UniversityScopeService universityScopeService;

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers(UserType userType, Boolean isActive, Long universityId,
                                                 Long instituteId, Long groupId, String searchQuery,
                                                 String actorEmail) {
        Users actor = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (actor.getUserType() != UserType.ADMIN) {
            throw new org.example.exception.AccessDeniedException("Список пользователей доступен только администратору");
        }
        Long adminUni = universityScopeService.requireAdminUniversityId(actorEmail);
        final String q = searchQuery == null ? null : searchQuery.trim().toLowerCase();
        return usersRepository.findAll().stream()
                .filter(u -> userType == null || u.getUserType() == userType)
                .filter(u -> isActive == null || Objects.equals(isActive, u.getIsActive()))
                .filter(u -> groupId == null || matchesGroup(u, groupId))
                .filter(u -> instituteId == null || matchesInstitute(u, instituteId))
                .filter(u -> matchesUniversity(u, adminUni))
                .filter(u -> q == null || q.isEmpty() || matchesNameSearch(u, q))
                .map(this::toFullResponse)
                .collect(Collectors.toList());
    }

    private boolean matchesNameSearch(Users u, String needleLower) {
        String ln = u.getLastName() != null ? u.getLastName().toLowerCase() : "";
        String fn = u.getFirstName() != null ? u.getFirstName().toLowerCase() : "";
        String mn = u.getMiddleName() != null ? u.getMiddleName().toLowerCase() : "";
        String full = (ln + " " + fn + " " + mn).trim();
        return ln.contains(needleLower) || fn.contains(needleLower) || mn.contains(needleLower)
                || full.contains(needleLower);
    }

    private boolean matchesGroup(Users u, Long groupId) {
        if (u.getUserType() != UserType.STUDENT) {
            return false;
        }
        return studentProfileRepository.findByUserId(u.getId())
                .map(sp -> sp.getGroup().getId().equals(groupId))
                .orElse(false);
    }

    private boolean matchesInstitute(Users u, Long instituteId) {
        return switch (u.getUserType()) {
            case STUDENT -> studentProfileRepository.findByUserId(u.getId())
                    .map(sp -> sp.getInstitute().getId().equals(instituteId))
                    .orElse(false);
            case TEACHER -> teacherProfileRepository.findByUserId(u.getId())
                    .map(tp -> tp.getInstitute() != null && tp.getInstitute().getId().equals(instituteId))
                    .orElse(false);
            default -> false;
        };
    }

    private boolean matchesUniversity(Users u, Long universityId) {
        return switch (u.getUserType()) {
            case ADMIN -> adminProfileRepository.findByUserId(u.getId())
                    .map(ap -> ap.getUniversity().getId().equals(universityId))
                    .orElse(false);
            case STUDENT -> studentProfileRepository.findByUserId(u.getId())
                    .map(sp -> sp.getInstitute().getUniversity().getId().equals(universityId))
                    .orElse(false);
            case TEACHER -> teacherProfileRepository.findByUserId(u.getId())
                    .map(tp -> tp.getInstitute() != null
                            && tp.getInstitute().getUniversity().getId().equals(universityId))
                    .orElse(false);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long id, String actorEmail) {
        Long adminUni = universityScopeService.requireAdminUniversityId(actorEmail);
        universityScopeService.assertUserInUniversity(id, adminUni);
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));
        return toFullResponse(user);
    }

    @Override
    @Transactional
    public void activateUser(Long id, String adminEmail) {
        Long adminUni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertUserInUniversity(id, adminUni);
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Пользователь уже активен");
        }

        user.setIsActive(true);
        usersRepository.save(user);

        Long adminId = resolveUserId(adminEmail);
        auditService.log(adminId, "ACTIVATE_USER", "Users", user.getId(),
                "Активирован пользователь " + user.getEmail());
    }

    @Override
    @Transactional
    public void deactivateUser(Long id, String adminEmail) {
        Long adminUni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertUserInUniversity(id, adminUni);
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadRequestException("Пользователь уже деактивирован");
        }

        Users admin = usersRepository.findByEmail(adminEmail).orElse(null);
        if (admin != null && admin.getId().equals(id)) {
            throw new BadRequestException("Нельзя деактивировать собственный аккаунт");
        }

        user.setIsActive(false);
        usersRepository.save(user);

        Long adminId = resolveUserId(adminEmail);
        auditService.log(adminId, "DEACTIVATE_USER", "Users", user.getId(),
                "Деактивирован пользователь " + user.getEmail());
    }

    private Long resolveUserId(String email) {
        if (email == null) return null;
        return usersRepository.findByEmail(email).map(Users::getId).orElse(null);
    }

    private UserProfileResponse toResponse(Users user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .userType(user.getUserType())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserProfileResponse toFullResponse(Users user) {
        UserProfileResponse.UserProfileResponseBuilder builder = toResponse(user).toBuilder();

        switch (user.getUserType()) {
            case STUDENT -> studentProfileRepository.findFetchedByUserId(user.getId()).ifPresent(profile ->
                    builder.studentProfile(UserProfileResponse.StudentProfileInfo.builder()
                            .groupId(profile.getGroup().getId())
                            .groupName(profile.getGroup().getName())
                            .instituteId(profile.getInstitute().getId())
                            .instituteName(profile.getInstitute().getName())
                            .build()));
            case TEACHER -> teacherProfileRepository.findByUserId(user.getId()).ifPresent(profile ->
                    builder.teacherProfile(UserProfileResponse.TeacherProfileInfo.builder()
                            .teacherProfileId(profile.getId())
                            .instituteId(profile.getInstitute() != null ? profile.getInstitute().getId() : null)
                            .instituteName(profile.getInstitute() != null ? profile.getInstitute().getName() : null)
                            .position(profile.getPosition())
                            .build()));
            case ADMIN -> adminProfileRepository.findFetchedByUserId(user.getId()).ifPresent(profile ->
                    builder.adminProfile(UserProfileResponse.AdminProfileInfo.builder()
                            .universityId(profile.getUniversity() != null ? profile.getUniversity().getId() : null)
                            .universityName(profile.getUniversity() != null ? profile.getUniversity().getName() : null)
                            .role("ADMIN")
                            .build()));
        }

        return builder.build();
    }
}
