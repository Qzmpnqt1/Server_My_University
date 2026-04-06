package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityScopeServiceImpl implements UniversityScopeService {

    private final UsersRepository usersRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final InstituteRepository instituteRepository;
    private final StudyDirectionRepository studyDirectionRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final ClassroomRepository classroomRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final ScheduleRepository scheduleRepository;

    private static boolean teacherBelongsToUniversity(TeacherProfile tp, Long universityId) {
        if (tp.getUniversity() != null && tp.getUniversity().getId().equals(universityId)) {
            return true;
        }
        if (tp.getInstitute() != null && tp.getInstitute().getUniversity() != null
                && tp.getInstitute().getUniversity().getId().equals(universityId)) {
            return true;
        }
        return false;
    }

    @Override
    public AdminQueryScope resolveAdminQueryScope(String viewerEmail, Long requestUniversityId) {
        Users u = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (u.getUserType() == UserType.ADMIN) {
            Long campus = requireCampusUniversityId(viewerEmail);
            if (requestUniversityId != null && !requestUniversityId.equals(campus)) {
                throw new AccessDeniedException("Нет доступа к данным другого вуза");
            }
            return new AdminQueryScope(false, campus);
        }
        if (u.getUserType() == UserType.SUPER_ADMIN) {
            if (requestUniversityId == null) {
                return new AdminQueryScope(true, null);
            }
            return new AdminQueryScope(false, requestUniversityId);
        }
        throw new AccessDeniedException("Требуются права администратора");
    }

    @Override
    public Set<Long> allUserIdsInUniversity(Long universityId) {
        Set<Long> ids = new HashSet<>();
        ids.addAll(studentProfileRepository.findUserIdsByUniversityId(universityId));
        ids.addAll(teacherProfileRepository.findUserIdsByUniversityId(universityId));
        ids.addAll(adminProfileRepository.findUserIdsByUniversityId(universityId));
        return ids;
    }

    @Override
    public boolean isSuperAdmin(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return usersRepository.findByEmail(email)
                .map(u -> u.getUserType() == UserType.SUPER_ADMIN)
                .orElse(false);
    }

    @Override
    public void requireAdminOrSuperAdmin(String email) {
        Users u = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (u.getUserType() != UserType.ADMIN && u.getUserType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Требуются права администратора");
        }
    }

    @Override
    public Long requireCampusUniversityId(String adminEmail) {
        Users u = usersRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (u.getUserType() != UserType.ADMIN) {
            throw new AccessDeniedException("Операция доступна только администратору вуза");
        }
        return adminProfileRepository.findFetchedByUserId(u.getId())
                .map(ap -> {
                    if (ap.getUniversity() == null) {
                        throw new AccessDeniedException("Администратор не привязан к вузу");
                    }
                    return ap.getUniversity().getId();
                })
                .orElseThrow(() -> new AccessDeniedException("Администратор не привязан к вузу"));
    }

    @Override
    public Long resolveMutationTargetUniversity(String actorEmail, Long requestedUniversityId) {
        requireAdminOrSuperAdmin(actorEmail);
        if (requestedUniversityId == null) {
            throw new BadRequestException("Не указан идентификатор вуза");
        }
        Users u = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (u.getUserType() == UserType.SUPER_ADMIN) {
            return requestedUniversityId;
        }
        Long my = requireCampusUniversityId(actorEmail);
        assertUniversityMatches(requestedUniversityId, my);
        return my;
    }

    @Override
    public Long enforceAccessToEntityUniversity(String actorEmail, Long entityUniversityId) {
        requireAdminOrSuperAdmin(actorEmail);
        if (entityUniversityId == null) {
            throw new BadRequestException("Не задан вуз сущности");
        }
        Users u = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (u.getUserType() == UserType.SUPER_ADMIN) {
            return entityUniversityId;
        }
        Long my = requireCampusUniversityId(actorEmail);
        assertUniversityMatches(entityUniversityId, my);
        return my;
    }

    @Override
    public AdminListScope resolveAdminListScope(String viewerEmail, Long queryUniversityId) {
        Users u = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (u.getUserType() == UserType.ADMIN) {
            Long my = requireCampusUniversityId(viewerEmail);
            if (queryUniversityId != null && !queryUniversityId.equals(my)) {
                throw new AccessDeniedException("Нет доступа к данным другого вуза");
            }
            return new AdminListScope(false, my);
        }
        if (u.getUserType() == UserType.SUPER_ADMIN) {
            if (queryUniversityId != null) {
                return new AdminListScope(false, queryUniversityId);
            }
            return new AdminListScope(true, null);
        }
        throw new AccessDeniedException("Требуются права администратора");
    }

    @Override
    public boolean userBelongsToUniversity(Long userId, Long universityId) {
        Users u = usersRepository.findById(userId).orElse(null);
        if (u == null || u.getUserType() == null) {
            return false;
        }
        return switch (u.getUserType()) {
            case STUDENT -> studentProfileRepository.findByUserId(userId)
                    .map(sp -> {
                        if (sp.getInstitute() == null || sp.getInstitute().getUniversity() == null) {
                            return false;
                        }
                        return sp.getInstitute().getUniversity().getId().equals(universityId);
                    })
                    .orElse(false);
            case TEACHER -> teacherProfileRepository.findByUserId(userId)
                    .map(tp -> teacherBelongsToUniversity(tp, universityId))
                    .orElse(false);
            case ADMIN, SUPER_ADMIN -> adminProfileRepository.findByUserId(userId)
                    .map(ap -> ap.getUniversity() != null && ap.getUniversity().getId().equals(universityId))
                    .orElse(false);
        };
    }

    @Override
    public void assertUserInUniversity(Long userId, Long universityId) {
        if (!userBelongsToUniversity(userId, universityId)) {
            throw new AccessDeniedException("Пользователь не относится к вашему вузу");
        }
    }

    @Override
    public void assertInstituteInUniversity(Long instituteId, Long universityId) {
        Institute i = instituteRepository.findById(instituteId)
                .orElseThrow(() -> new ResourceNotFoundException("Институт не найден"));
        if (!i.getUniversity().getId().equals(universityId)) {
            throw new AccessDeniedException("Институт не относится к вашему вузу");
        }
    }

    @Override
    public void assertStudyDirectionInUniversity(Long directionId, Long universityId) {
        StudyDirection d = studyDirectionRepository.findById(directionId)
                .orElseThrow(() -> new ResourceNotFoundException("Направление не найдено"));
        if (!d.getInstitute().getUniversity().getId().equals(universityId)) {
            throw new AccessDeniedException("Направление не относится к вашему вузу");
        }
    }

    @Override
    public void assertAcademicGroupInUniversity(Long groupId, Long universityId) {
        AcademicGroup g = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
        if (!g.getDirection().getInstitute().getUniversity().getId().equals(universityId)) {
            throw new AccessDeniedException("Группа не относится к вашему вузу");
        }
    }

    @Override
    public void assertClassroomInUniversity(Long classroomId, Long universityId) {
        Classroom c = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Аудитория не найдена"));
        if (!c.getUniversity().getId().equals(universityId)) {
            throw new AccessDeniedException("Аудитория не относится к вашему вузу");
        }
    }

    @Override
    public void assertSubjectDirectionInUniversity(Long subjectDirectionId, Long universityId) {
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));
        if (!sid.getDirection().getInstitute().getUniversity().getId().equals(universityId)) {
            throw new AccessDeniedException("Предмет не относится к вашему вузу");
        }
    }

    @Override
    public void assertUniversityMatches(Long universityId, Long adminUniversityId) {
        if (!adminUniversityId.equals(universityId)) {
            throw new AccessDeniedException("Нет доступа к данным другого вуза");
        }
    }

    @Override
    public void assertRegistrationRequestInUniversity(Long requestId, Long universityId) {
        var r = registrationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
        if (r.getUniversity() == null || !r.getUniversity().getId().equals(universityId)) {
            throw new AccessDeniedException("Заявка не относится к вашему вузу");
        }
    }

    @Override
    public boolean teacherUserInUniversity(Long teacherUserId, Long universityId) {
        if (userBelongsToUniversity(teacherUserId, universityId)) {
            return true;
        }
        Users u = usersRepository.findById(teacherUserId).orElse(null);
        if (u == null || u.getUserType() != UserType.TEACHER) {
            return false;
        }
        return scheduleRepository.existsByTeacherUserIdAndUniversityId(teacherUserId, universityId);
    }
}
