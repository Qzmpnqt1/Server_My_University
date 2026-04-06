package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.mapper.TeacherProfileInfoMapper;
import org.example.dto.request.CreateAdminAccountRequest;
import org.example.dto.response.UserProfileResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.AuditService;
import org.example.service.UniversityScopeService;
import org.example.service.UserService;
import org.example.util.RussianSort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UniversityRepository universityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final UniversityScopeService universityScopeService;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherProfileInfoMapper teacherProfileInfoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers(UserType userType, Boolean isActive, Long universityId,
                                                 Long instituteId, Long groupId, String searchQuery,
                                                 String actorEmail) {
        Users actor = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (actor.getUserType() != UserType.ADMIN && actor.getUserType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Список пользователей доступен только администратору");
        }
        Long scopeUniversityId = null;
        if (actor.getUserType() == UserType.ADMIN) {
            scopeUniversityId = universityScopeService.requireCampusUniversityId(actorEmail);
            if (universityId != null && !universityId.equals(scopeUniversityId)) {
                throw new AccessDeniedException("Нет доступа к данным другого вуза");
            }
        } else if (actor.getUserType() == UserType.SUPER_ADMIN) {
            scopeUniversityId = universityId;
        }
        final String q = searchQuery == null ? null : searchQuery.trim().toLowerCase();
        final Long filterUni = scopeUniversityId;
        List<UserProfileResponse> out = usersRepository.findAll().stream()
                .filter(u -> userType == null || u.getUserType() == userType)
                .filter(u -> isActive == null || Objects.equals(isActive, u.getIsActive()))
                .filter(u -> groupId == null || matchesGroup(u, groupId))
                .filter(u -> instituteId == null || matchesInstitute(u, instituteId))
                .filter(u -> filterUni == null || matchesUniversity(u, filterUni))
                .filter(u -> q == null || q.isEmpty() || matchesNameSearch(u, q))
                .map(this::toFullResponse)
                .collect(Collectors.toList());
        out.sort(RussianSort.userProfiles());
        return out;
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
                    .map(tp -> (tp.getInstitute() != null && tp.getInstitute().getId().equals(instituteId))
                            || teacherSubjectRepository.teacherTeachesInInstitute(tp.getId(), instituteId))
                    .orElse(false);
            default -> false;
        };
    }

    private boolean matchesUniversity(Users u, Long universityId) {
        return switch (u.getUserType()) {
            case ADMIN, SUPER_ADMIN -> adminProfileRepository.findByUserId(u.getId())
                    .map(ap -> ap.getUniversity() != null && ap.getUniversity().getId().equals(universityId))
                    .orElse(false);
            case STUDENT -> studentProfileRepository.findByUserId(u.getId())
                    .map(sp -> sp.getInstitute().getUniversity().getId().equals(universityId))
                    .orElse(false);
            case TEACHER -> teacherProfileRepository.findByUserId(u.getId())
                    .map(tp -> teacherInUniversity(tp, universityId))
                    .orElse(false);
        };
    }

    @Override
    @Transactional
    public UserProfileResponse createAdminAccount(CreateAdminAccountRequest req, String actorEmail) {
        Users actor = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (actor.getUserType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException(
                    "Создание администраторских учётных записей доступно только супер-администратору");
        }
        if (req.getUserType() != UserType.ADMIN && req.getUserType() != UserType.SUPER_ADMIN) {
            throw new BadRequestException("Можно создать только ADMIN или SUPER_ADMIN");
        }
        if (req.getUserType() == UserType.SUPER_ADMIN && req.getUniversityId() != null) {
            throw new BadRequestException("Для супер-администратора вуз указывать нельзя");
        }
        if (req.getUserType() == UserType.ADMIN && req.getUniversityId() == null) {
            throw new BadRequestException("Для администратора вуза необходимо указать universityId");
        }
        String email = req.getEmail().trim();
        if (usersRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        University university = null;
        if (req.getUserType() == UserType.ADMIN) {
            university = universityRepository.findById(req.getUniversityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Университет не найден"));
        }
        Users user = Users.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .middleName(req.getMiddleName() != null && !req.getMiddleName().isBlank()
                        ? req.getMiddleName().trim()
                        : null)
                .userType(req.getUserType())
                .isActive(true)
                .build();
        user = usersRepository.save(user);
        AdminProfile adminProfile = AdminProfile.builder()
                .user(user)
                .university(university)
                .role(req.getUserType() == UserType.SUPER_ADMIN ? "SUPER_ADMIN" : "ADMIN")
                .build();
        adminProfileRepository.save(adminProfile);
        Long adminId = resolveUserId(actorEmail);
        auditService.log(adminId, "CREATE_ADMIN_ACCOUNT", "Users", user.getId(),
                "Создан " + req.getUserType() + ": " + email);
        return toFullResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long id, String actorEmail) {
        Users actor = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (actor.getUserType() == UserType.SUPER_ADMIN) {
            Users user = usersRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));
            return toFullResponse(user);
        }
        Long adminUni = universityScopeService.requireCampusUniversityId(actorEmail);
        universityScopeService.assertUserInUniversity(id, adminUni);
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));
        return toFullResponse(user);
    }

    @Override
    @Transactional
    public void activateUser(Long id, String adminEmail) {
        if (universityScopeService.isSuperAdmin(adminEmail)) {
            Users user = usersRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));
            activateUserCore(user, adminEmail);
            return;
        }
        Long adminUni = universityScopeService.requireCampusUniversityId(adminEmail);
        universityScopeService.assertUserInUniversity(id, adminUni);
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));
        activateUserCore(user, adminEmail);
    }

    private void activateUserCore(Users user, String adminEmail) {
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
        if (universityScopeService.isSuperAdmin(adminEmail)) {
            Users user = usersRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));
            deactivateUserCore(user, adminEmail);
            return;
        }
        Long adminUni = universityScopeService.requireCampusUniversityId(adminEmail);
        universityScopeService.assertUserInUniversity(id, adminUni);
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с id: " + id));
        deactivateUserCore(user, adminEmail);
    }

    private void deactivateUserCore(Users user, String adminEmail) {
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadRequestException("Пользователь уже деактивирован");
        }
        Users admin = usersRepository.findByEmail(adminEmail).orElse(null);
        if (admin != null && admin.getId().equals(user.getId())) {
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
                            .course(profile.getGroup().getCourse())
                            .instituteId(profile.getInstitute().getId())
                            .instituteName(profile.getInstitute().getName())
                            .build()));
            case TEACHER -> teacherProfileRepository.findByUserId(user.getId()).ifPresent(profile ->
                    builder.teacherProfile(teacherProfileInfoMapper.toInfo(profile)));
            case ADMIN -> adminProfileRepository.findFetchedByUserId(user.getId()).ifPresent(profile ->
                    builder.adminProfile(UserProfileResponse.AdminProfileInfo.builder()
                            .universityId(profile.getUniversity() != null ? profile.getUniversity().getId() : null)
                            .universityName(profile.getUniversity() != null ? profile.getUniversity().getName() : null)
                            .role("ADMIN")
                            .build()));
            case SUPER_ADMIN -> adminProfileRepository.findFetchedByUserId(user.getId()).ifPresent(profile ->
                    builder.adminProfile(UserProfileResponse.AdminProfileInfo.builder()
                            .universityId(profile.getUniversity() != null ? profile.getUniversity().getId() : null)
                            .universityName(profile.getUniversity() != null ? profile.getUniversity().getName() : null)
                            .role("SUPER_ADMIN")
                            .build()));
        }

        return builder.build();
    }

    private boolean teacherInUniversity(TeacherProfile tp, Long universityId) {
        if (tp.getUniversity() != null && tp.getUniversity().getId().equals(universityId)) {
            return true;
        }
        if (tp.getInstitute() != null && tp.getInstitute().getUniversity() != null
                && tp.getInstitute().getUniversity().getId().equals(universityId)) {
            return true;
        }
        return teacherSubjectRepository.findDistinctInstitutesForTeacher(tp.getId()).stream()
                .anyMatch(i -> i.getUniversity().getId().equals(universityId));
    }
}
