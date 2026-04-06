package org.example.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.ApproveRejectRequest;
import org.example.dto.request.GuestRegistrationLookupRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.request.UpdatePendingRegistrationRequest;
import org.example.dto.response.GuestRegistrationStatusResponse;
import org.example.dto.response.RegistrationRequestResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.AuditService;
import org.example.service.NotificationService;
import org.example.service.RegistrationService;
import org.example.service.UniversityScopeService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRequestRepository registrationRequestRepository;
    private final UsersRepository usersRepository;
    private final UniversityRepository universityRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UniversityScopeService universityScopeService;

    @Override
    @Transactional
    public void submitRegistration(RegisterRequest request) {
        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        if (registrationRequestRepository.existsByEmailAndStatus(request.getEmail(), RegistrationStatus.PENDING)) {
            throw new ConflictException("Заявка с таким email уже находится на рассмотрении");
        }

        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new ResourceNotFoundException("Университет не найден"));

        AcademicGroup group = null;
        Institute institute = null;

        if (request.getUserType() == UserType.STUDENT) {
            if (request.getGroupId() == null) {
                throw new BadRequestException("Для студента необходимо указать группу");
            }
            group = academicGroupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Академическая группа не найдена"));
        } else if (request.getUserType() == UserType.TEACHER) {
            if (request.getGroupId() != null) {
                throw new BadRequestException(
                        "Для преподавателя группа не указывается; институты и предметы назначает администратор после одобрения");
            }
            // Институт и дисциплины не задаются при регистрации преподавателя
            institute = null;
        }

        if (request.getUserType() == UserType.ADMIN || request.getUserType() == UserType.SUPER_ADMIN) {
            throw new BadRequestException("Регистрация администратора через заявку запрещена");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        RegistrationRequest registrationRequest = RegistrationRequest.builder()
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .userType(request.getUserType())
                .status(RegistrationStatus.PENDING)
                .university(university)
                .group(group)
                .institute(institute)
                .build();

        registrationRequestRepository.save(registrationRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public GuestRegistrationStatusResponse lookupRegistrationStatus(GuestRegistrationLookupRequest request) {
        Optional<RegistrationRequest> opt =
                registrationRequestRepository.findFirstByEmailOrderByCreatedAtDesc(request.getEmail());
        if (opt.isEmpty() || !passwordEncoder.matches(request.getPassword(), opt.get().getPasswordHash())) {
            throw new BadRequestException("Неверные учётные данные");
        }
        RegistrationRequest r = opt.get();
        return GuestRegistrationStatusResponse.builder()
                .id(r.getId())
                .status(r.getStatus())
                .userType(r.getUserType())
                .universityId(r.getUniversity() != null ? r.getUniversity().getId() : null)
                .groupId(r.getGroup() != null ? r.getGroup().getId() : null)
                .instituteId(r.getInstitute() != null ? r.getInstitute().getId() : null)
                .rejectionReason(r.getRejectionReason())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void updatePendingRegistration(UpdatePendingRegistrationRequest body) {
        Optional<RegistrationRequest> opt =
                registrationRequestRepository.findFirstByEmailOrderByCreatedAtDesc(body.getUpdated().getEmail());
        if (opt.isEmpty() || !passwordEncoder.matches(body.getCurrentPassword(), opt.get().getPasswordHash())) {
            throw new BadRequestException("Неверные учётные данные");
        }
        RegistrationRequest r = opt.get();
        if (r.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Редактирование возможно только для заявок в статусе PENDING");
        }
        RegisterRequest upd = body.getUpdated();
        applyRegistrationPayload(r, upd);
        registrationRequestRepository.save(r);
    }

    private void applyRegistrationPayload(RegistrationRequest target, RegisterRequest upd) {
        if (usersRepository.existsByEmail(upd.getEmail()) && !upd.getEmail().equalsIgnoreCase(target.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        University university = universityRepository.findById(upd.getUniversityId())
                .orElseThrow(() -> new ResourceNotFoundException("Университет не найден"));

        AcademicGroup group = null;
        Institute institute = null;
        if (upd.getUserType() == UserType.STUDENT) {
            if (upd.getGroupId() == null) {
                throw new BadRequestException("Для студента необходимо указать группу");
            }
            group = academicGroupRepository.findById(upd.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Академическая группа не найдена"));
        } else if (upd.getUserType() == UserType.TEACHER) {
            if (upd.getGroupId() != null) {
                throw new BadRequestException(
                        "Для преподавателя группа не указывается; институты и предметы назначает администратор после одобрения");
            }
            institute = null;
        }

        if (upd.getUserType() == UserType.ADMIN || upd.getUserType() == UserType.SUPER_ADMIN) {
            throw new BadRequestException("Регистрация администратора через заявку запрещена");
        }

        target.setEmail(upd.getEmail());
        target.setPasswordHash(passwordEncoder.encode(upd.getPassword()));
        target.setFirstName(upd.getFirstName());
        target.setLastName(upd.getLastName());
        target.setMiddleName(upd.getMiddleName());
        target.setUserType(upd.getUserType());
        target.setUniversity(university);
        target.setGroup(group);
        target.setInstitute(institute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationRequestResponse> getAllRequests(RegistrationStatus status, UserType userType,
                                                            Long universityId, Long instituteId, String adminEmail) {
        Users actor = usersRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        universityScopeService.requireAdminOrSuperAdmin(adminEmail);
        Long filterUni;
        if (actor.getUserType() == UserType.ADMIN) {
            filterUni = universityScopeService.requireCampusUniversityId(adminEmail);
            if (universityId != null && !universityId.equals(filterUni)) {
                throw new org.example.exception.AccessDeniedException("Нет доступа к заявкам другого вуза");
            }
        } else {
            filterUni = universityId;
        }
        Specification<RegistrationRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (userType != null) {
                predicates.add(cb.equal(root.get("userType"), userType));
            }
            if (filterUni != null) {
                predicates.add(cb.equal(root.get("university").get("id"), filterUni));
            }
            if (instituteId != null) {
                predicates.add(cb.equal(root.join("institute").get("id"), instituteId));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return registrationRequestRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationRequestResponse getRequestById(Long id, String adminEmail) {
        Users actor = usersRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        universityScopeService.requireAdminOrSuperAdmin(adminEmail);
        if (actor.getUserType() == UserType.ADMIN) {
            Long adminUni = universityScopeService.requireCampusUniversityId(adminEmail);
            universityScopeService.assertRegistrationRequestInUniversity(id, adminUni);
        }
        RegistrationRequest request = registrationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка на регистрацию не найдена"));
        return mapToResponse(request);
    }

    @Override
    @Transactional
    public void approveRequest(Long id, String adminEmail) {
        RegistrationRequest request = registrationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка на регистрацию не найдена"));

        universityScopeService.requireAdminOrSuperAdmin(adminEmail);
        if (usersRepository.findByEmail(adminEmail).orElseThrow().getUserType() == UserType.ADMIN) {
            Long adminUni = universityScopeService.requireCampusUniversityId(adminEmail);
            universityScopeService.assertRegistrationRequestInUniversity(id, adminUni);
        }

        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Можно одобрить только заявку со статусом PENDING");
        }

        if (usersRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        Users user = Users.builder()
                .email(request.getEmail())
                .passwordHash(request.getPasswordHash())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .userType(request.getUserType())
                .isActive(true)
                .build();

        user = usersRepository.save(user);

        createProfileForUser(user, request);

        request.setStatus(RegistrationStatus.APPROVED);
        registrationRequestRepository.save(request);

        Long adminId = resolveUserId(adminEmail);
        auditService.log(adminId, "APPROVE_REGISTRATION", "RegistrationRequest", request.getId(),
                "Заявка одобрена для " + request.getEmail());
        notificationService.notifyRegistrationApproved(request.getEmail());
    }

    @Override
    @Transactional
    public void rejectRequest(Long id, ApproveRejectRequest rejectBody, String adminEmail) {
        RegistrationRequest request = registrationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка на регистрацию не найдена"));

        universityScopeService.requireAdminOrSuperAdmin(adminEmail);
        if (usersRepository.findByEmail(adminEmail).orElseThrow().getUserType() == UserType.ADMIN) {
            Long adminUni = universityScopeService.requireCampusUniversityId(adminEmail);
            universityScopeService.assertRegistrationRequestInUniversity(id, adminUni);
        }

        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new BadRequestException("Можно отклонить только заявку со статусом PENDING");
        }

        request.setStatus(RegistrationStatus.REJECTED);
        request.setRejectionReason(rejectBody.getRejectionReason());
        registrationRequestRepository.save(request);

        Long adminId = resolveUserId(adminEmail);
        auditService.log(adminId, "REJECT_REGISTRATION", "RegistrationRequest", request.getId(),
                "Заявка отклонена для " + request.getEmail() + ": " + rejectBody.getRejectionReason());
        notificationService.notifyRegistrationRejected(request.getEmail(), rejectBody.getRejectionReason());
    }

    private void createProfileForUser(Users user, RegistrationRequest request) {
        switch (request.getUserType()) {
            case STUDENT -> {
                Institute institute = request.getGroup().getDirection().getInstitute();
                StudentProfile studentProfile = StudentProfile.builder()
                        .user(user)
                        .group(request.getGroup())
                        .institute(institute)
                        .build();
                studentProfileRepository.save(studentProfile);
            }
            case TEACHER -> {
                TeacherProfile teacherProfile = TeacherProfile.builder()
                        .user(user)
                        .university(request.getUniversity())
                        .institute(null)
                        .build();
                teacherProfileRepository.save(teacherProfile);
            }
            case ADMIN -> {
                AdminProfile adminProfile = AdminProfile.builder()
                        .user(user)
                        .university(request.getUniversity())
                        .role("ADMIN")
                        .build();
                adminProfileRepository.save(adminProfile);
            }
        }
    }

    private Long resolveUserId(String email) {
        if (email == null) return null;
        return usersRepository.findByEmail(email).map(Users::getId).orElse(null);
    }

    private RegistrationRequestResponse mapToResponse(RegistrationRequest request) {
        return RegistrationRequestResponse.builder()
                .id(request.getId())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .userType(request.getUserType())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .universityId(request.getUniversity() != null ? request.getUniversity().getId() : null)
                .universityName(request.getUniversity() != null ? request.getUniversity().getName() : null)
                .groupId(request.getGroup() != null ? request.getGroup().getId() : null)
                .groupName(request.getGroup() != null ? request.getGroup().getName() : null)
                .instituteId(request.getInstitute() != null ? request.getInstitute().getId() : null)
                .instituteName(request.getInstitute() != null ? request.getInstitute().getName() : null)
                .createdAt(request.getCreatedAt())
                .build();
    }
}
