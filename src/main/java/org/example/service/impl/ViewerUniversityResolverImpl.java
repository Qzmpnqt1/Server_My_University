package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.AccessDeniedException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Users;
import org.example.repository.ScheduleRepository;
import org.example.repository.StudentProfileRepository;
import org.example.repository.TeacherProfileRepository;
import org.example.repository.TeacherSubjectRepository;
import org.example.repository.UsersRepository;
import org.example.service.UniversityScopeService;
import org.example.service.ViewerUniversityResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViewerUniversityResolverImpl implements ViewerUniversityResolver {

    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public long requireUniversityIdForEmail(String email) {
        Users u = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return switch (u.getUserType()) {
            case SUPER_ADMIN -> throw new AccessDeniedException(
                    "Для суперадминистратора укажите вуз в параметрах запроса (universityId / scopeUniversityId)");
            case ADMIN -> {
                long id = universityScopeService.requireCampusUniversityId(email);
                log.debug("ViewerUniversityResolver admin email={} universityId={}", email, id);
                yield id;
            }
            case STUDENT -> {
                long id = studentProfileRepository.findFetchedByUserId(u.getId())
                        .map(sp -> sp.getInstitute().getUniversity().getId())
                        .orElseThrow(() -> new AccessDeniedException("Профиль студента не найден"));
                log.debug("ViewerUniversityResolver student email={} universityId={}", email, id);
                yield id;
            }
            case TEACHER -> {
                long id = resolveTeacherUniversity(u.getId());
                log.debug("ViewerUniversityResolver teacher email={} universityId={}", email, id);
                yield id;
            }
            default -> throw new AccessDeniedException("Расписание недоступно для данного типа пользователя");
        };
    }

    private long resolveTeacherUniversity(Long userId) {
        var tpOpt = teacherProfileRepository.findFetchedByUserId(userId);
        if (tpOpt.isEmpty()) {
            throw new AccessDeniedException("Профиль преподавателя не найден");
        }
        var tp = tpOpt.get();
        if (tp.getUniversity() != null) {
            return tp.getUniversity().getId();
        }
        if (tp.getInstitute() != null && tp.getInstitute().getUniversity() != null) {
            return tp.getInstitute().getUniversity().getId();
        }
        var institutes = teacherSubjectRepository.findDistinctInstitutesForTeacher(tp.getId());
        if (!institutes.isEmpty()) {
            return institutes.get(0).getUniversity().getId();
        }
        var scheds = scheduleRepository.findByTeacherId(userId);
        if (!scheds.isEmpty()) {
            return scheds.get(0).getGroup().getDirection().getInstitute().getUniversity().getId();
        }
        throw new AccessDeniedException(
                "Не удалось определить вуз преподавателя: укажите институт в профиле или добавьте назначения/расписание");
    }
}
