package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.exception.AccessDeniedException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.ScheduleAuthorizationService;
import org.example.service.UniversityScopeService;
import org.example.service.ViewerUniversityResolver;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleAuthorizationServiceImpl implements ScheduleAuthorizationService {

    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;
    private final ViewerUniversityResolver viewerUniversityResolver;
    private final StudentProfileRepository studentProfileRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public void ensureAdmin(String email) {
        Users u = loadUser(email);
        if (u.getUserType() != UserType.ADMIN) {
            throw new AccessDeniedException("Требуются права администратора");
        }
    }

    @Override
    public void ensureCanViewGroupSchedule(String email, Long groupId) {
        Users u = loadUser(email);
        if (u.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(email);
            universityScopeService.assertAcademicGroupInUniversity(groupId, uni);
            return;
        }
        if (u.getUserType() == UserType.STUDENT || u.getUserType() == UserType.TEACHER) {
            long viewerUni = viewerUniversityResolver.requireUniversityIdForEmail(email);
            universityScopeService.assertAcademicGroupInUniversity(groupId, viewerUni);
            return;
        }
        throw new AccessDeniedException("Расписание недоступно");
    }

    @Override
    public void ensureCanViewTeacherSchedule(String email, Long teacherUserId) {
        Users u = loadUser(email);
        if (u.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(email);
            if (!universityScopeService.teacherUserInUniversity(teacherUserId, uni)) {
                throw new AccessDeniedException("Преподаватель не относится к вашему вузу");
            }
            return;
        }
        if (u.getUserType() == UserType.STUDENT || u.getUserType() == UserType.TEACHER) {
            long viewerUni = viewerUniversityResolver.requireUniversityIdForEmail(email);
            if (!universityScopeService.teacherUserInUniversity(teacherUserId, viewerUni)) {
                throw new AccessDeniedException("Нет доступа к расписанию этого преподавателя");
            }
            return;
        }
        throw new AccessDeniedException("Нет доступа к расписанию этого преподавателя");
    }

    @Override
    public void ensureCanViewClassroomSchedule(String email, Long classroomId) {
        Users u = loadUser(email);
        if (u.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(email);
            universityScopeService.assertClassroomInUniversity(classroomId, uni);
            return;
        }
        if (u.getUserType() == UserType.STUDENT || u.getUserType() == UserType.TEACHER) {
            long viewerUni = viewerUniversityResolver.requireUniversityIdForEmail(email);
            universityScopeService.assertClassroomInUniversity(classroomId, viewerUni);
            return;
        }
        throw new AccessDeniedException("Расписание аудитории недоступно");
    }

    @Override
    public void ensureCanViewScheduleEntry(String email, Long scheduleId) {
        Users u = loadUser(email);
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись расписания не найдена"));
        if (u.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(email);
            universityScopeService.assertAcademicGroupInUniversity(s.getGroup().getId(), uni);
            return;
        }
        if (u.getUserType() == UserType.STUDENT) {
            StudentProfile sp = studentProfileRepository.findFetchedByUserId(u.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Профиль студента не найден"));
            if (sp.getGroup().getId().equals(s.getGroup().getId())) {
                return;
            }
            throw new AccessDeniedException("Нет доступа к этой записи расписания");
        }
        if (u.getUserType() == UserType.TEACHER) {
            if (s.getTeacher().getId().equals(u.getId())) {
                return;
            }
            ensureCanViewGroupSchedule(email, s.getGroup().getId());
            return;
        }
        throw new AccessDeniedException("Нет доступа к этой записи расписания");
    }

    private Users loadUser(String email) {
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
