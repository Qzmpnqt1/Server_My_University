package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.ScheduleRequest;
import org.example.dto.response.ScheduleResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.AuditService;
import org.example.service.NotificationService;
import org.example.service.ScheduleService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final SubjectLessonTypeRepository subjectLessonTypeRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final UsersRepository usersRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UniversityScopeService universityScopeService;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllForAdmin(String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        return scheduleRepository.findAllByUniversityId(uni).stream()
                .map(ScheduleResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getById(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись расписания не найдена с id: " + id));
        return ScheduleResponseMapper.toResponse(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getByGroup(Long groupId, Integer weekNumber, Integer dayOfWeek) {
        academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Академическая группа не найдена"));

        List<Schedule> schedules = findByGroupFiltered(groupId, weekNumber, dayOfWeek);

        return schedules.stream()
                .map(ScheduleResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getByTeacher(Long teacherId, Integer weekNumber, Integer dayOfWeek) {
        usersRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));

        List<Schedule> schedules = findByTeacherFiltered(teacherId, weekNumber, dayOfWeek);

        return schedules.stream()
                .map(ScheduleResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getByClassroom(Long classroomId, Integer weekNumber, Integer dayOfWeek) {
        classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Аудитория не найдена"));
        List<Schedule> schedules = findByClassroomFiltered(classroomId, weekNumber, dayOfWeek);
        return schedules.stream()
                .map(ScheduleResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMySchedule(String email, Integer weekNumber, Integer dayOfWeek) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        switch (user.getUserType()) {
            case STUDENT -> {
                StudentProfile profile = studentProfileRepository.findFetchedByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Профиль студента не найден"));
                return getByGroup(profile.getGroup().getId(), weekNumber, dayOfWeek);
            }
            case TEACHER -> {
                return getByTeacher(user.getId(), weekNumber, dayOfWeek);
            }
            case ADMIN -> throw new AccessDeniedException(
                    "Для администратора используйте просмотр расписания по группе или преподавателю");
            default -> throw new BadRequestException("Расписание недоступно для данного типа пользователя");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getLinkedGroupsSchedule(String teacherEmail, Integer weekNumber, Integer dayOfWeek) {
        Users user = usersRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getUserType() != UserType.TEACHER) {
            throw new AccessDeniedException("Доступно только преподавателям");
        }
        TeacherProfile tp = teacherProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профиль преподавателя не найден"));

        LinkedHashSet<Long> groupIds = new LinkedHashSet<>();
        groupIds.addAll(scheduleRepository.findDistinctGroupIdsByTeacherUserId(user.getId()));
        for (TeacherSubject ts : teacherSubjectRepository.findByTeacherId(tp.getId())) {
            SubjectInDirection sid = ts.getSubjectInDirection();
            Long dirId = sid.getDirection().getId();
            academicGroupRepository.findByDirectionId(dirId).forEach(g -> groupIds.add(g.getId()));
        }

        Map<Long, ScheduleResponse> merged = new LinkedHashMap<>();
        for (Long gid : groupIds) {
            for (Schedule s : findByGroupFiltered(gid, weekNumber, dayOfWeek)) {
                merged.putIfAbsent(s.getId(), ScheduleResponseMapper.toResponse(s));
            }
        }
        return new ArrayList<>(merged.values());
    }

    @Override
    @Transactional
    public ScheduleResponse create(ScheduleRequest request, String actorEmail) {
        validateRequest(request);

        SubjectLessonType subjectType = subjectLessonTypeRepository.findById(request.getSubjectTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Тип занятия не найден"));
        Users teacher = usersRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        if (teacher.getUserType() != UserType.TEACHER) {
            throw new BadRequestException("Указанный пользователь не является преподавателем");
        }
        AcademicGroup group = academicGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Академическая группа не найдена"));
        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Аудитория не найдена"));

        Users actor = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (actor.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(actorEmail);
            universityScopeService.assertSubjectDirectionInUniversity(subjectType.getSubjectDirection().getId(), uni);
            universityScopeService.assertAcademicGroupInUniversity(request.getGroupId(), uni);
            universityScopeService.assertClassroomInUniversity(request.getClassroomId(), uni);
            universityScopeService.assertUserInUniversity(request.getTeacherId(), uni);
        }

        checkConflicts(request, null);

        Schedule schedule = Schedule.builder()
                .subjectType(subjectType)
                .teacher(teacher)
                .group(group)
                .classroom(classroom)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .weekNumber(request.getWeekNumber())
                .build();

        schedule = scheduleRepository.save(schedule);

        Long actorId = resolveUserId(actorEmail);
        auditService.log(actorId, "CREATE_SCHEDULE", "Schedule", schedule.getId(),
                "Создана запись расписания: группа " + group.getId() + ", преподаватель " + teacher.getId());
        notificationService.notifyScheduleChanged(group.getId(), teacher.getId());

        return ScheduleResponseMapper.toResponse(schedule);
    }

    @Override
    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest request, String actorEmail) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись расписания не найдена"));

        validateRequest(request);

        SubjectLessonType subjectType = subjectLessonTypeRepository.findById(request.getSubjectTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Тип занятия не найден"));
        Users teacher = usersRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель не найден"));
        if (teacher.getUserType() != UserType.TEACHER) {
            throw new BadRequestException("Указанный пользователь не является преподавателем");
        }
        AcademicGroup group = academicGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Академическая группа не найдена"));
        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new ResourceNotFoundException("Аудитория не найдена"));

        Users actor = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (actor.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(actorEmail);
            universityScopeService.assertSubjectDirectionInUniversity(subjectType.getSubjectDirection().getId(), uni);
            universityScopeService.assertAcademicGroupInUniversity(request.getGroupId(), uni);
            universityScopeService.assertClassroomInUniversity(request.getClassroomId(), uni);
            universityScopeService.assertUserInUniversity(request.getTeacherId(), uni);
        }

        checkConflicts(request, id);

        schedule.setSubjectType(subjectType);
        schedule.setTeacher(teacher);
        schedule.setGroup(group);
        schedule.setClassroom(classroom);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setWeekNumber(request.getWeekNumber());

        schedule = scheduleRepository.save(schedule);

        Long actorId = resolveUserId(actorEmail);
        auditService.log(actorId, "UPDATE_SCHEDULE", "Schedule", schedule.getId(), "Обновлена запись расписания");
        notificationService.notifyScheduleChanged(group.getId(), teacher.getId());

        return ScheduleResponseMapper.toResponse(schedule);
    }

    @Override
    @Transactional
    public void delete(Long id, String actorEmail) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись расписания не найдена"));
        Users actor = usersRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (actor.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(actorEmail);
            universityScopeService.assertAcademicGroupInUniversity(schedule.getGroup().getId(), uni);
        }
        Long groupId = schedule.getGroup().getId();
        Long teacherId = schedule.getTeacher().getId();
        scheduleRepository.delete(schedule);

        Long actorId = resolveUserId(actorEmail);
        auditService.log(actorId, "DELETE_SCHEDULE", "Schedule", id, "Удалена запись расписания");
        notificationService.notifyScheduleChanged(groupId, teacherId);
    }

    private Long resolveUserId(String email) {
        if (email == null) {
            return null;
        }
        return usersRepository.findByEmail(email).map(Users::getId).orElse(null);
    }

    private List<Schedule> findByGroupFiltered(Long groupId, Integer weekNumber, Integer dayOfWeek) {
        if (weekNumber != null && dayOfWeek != null) {
            return scheduleRepository.findByGroupIdAndWeekNumberAndDayOfWeek(groupId, weekNumber, dayOfWeek);
        } else if (weekNumber != null) {
            return scheduleRepository.findByGroupIdAndWeekNumber(groupId, weekNumber);
        } else if (dayOfWeek != null) {
            return scheduleRepository.findByGroupIdAndDayOfWeek(groupId, dayOfWeek);
        } else {
            return scheduleRepository.findByGroupId(groupId);
        }
    }

    private List<Schedule> findByTeacherFiltered(Long teacherId, Integer weekNumber, Integer dayOfWeek) {
        if (weekNumber != null && dayOfWeek != null) {
            return scheduleRepository.findByTeacherIdAndWeekNumberAndDayOfWeek(teacherId, weekNumber, dayOfWeek);
        } else if (weekNumber != null) {
            return scheduleRepository.findByTeacherIdAndWeekNumber(teacherId, weekNumber);
        } else if (dayOfWeek != null) {
            return scheduleRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek);
        } else {
            return scheduleRepository.findByTeacherId(teacherId);
        }
    }

    private List<Schedule> findByClassroomFiltered(Long classroomId, Integer weekNumber, Integer dayOfWeek) {
        if (weekNumber != null && dayOfWeek != null) {
            return scheduleRepository.findByClassroomIdAndWeekNumberAndDayOfWeek(classroomId, weekNumber, dayOfWeek);
        } else if (weekNumber != null) {
            return scheduleRepository.findByClassroomIdAndWeekNumber(classroomId, weekNumber);
        } else if (dayOfWeek != null) {
            return scheduleRepository.findByClassroomIdAndDayOfWeek(classroomId, dayOfWeek);
        } else {
            return scheduleRepository.findByClassroomId(classroomId);
        }
    }

    private void validateRequest(ScheduleRequest request) {
        if (request.getDayOfWeek() == null || request.getDayOfWeek() < 1 || request.getDayOfWeek() > 7) {
            throw new BadRequestException("День недели должен быть от 1 до 7");
        }
        if (request.getWeekNumber() == null || request.getWeekNumber() < 1 || request.getWeekNumber() > 2) {
            throw new BadRequestException("Номер недели должен быть 1 или 2");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Время начала и окончания обязательны");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Время начала должно быть раньше времени окончания");
        }
    }

    private void checkConflicts(ScheduleRequest request, Long excludeId) {
        List<Schedule> teacherConflicts = scheduleRepository.findTeacherConflicts(
                request.getDayOfWeek(), request.getWeekNumber(),
                request.getStartTime(), request.getEndTime(),
                request.getTeacherId(), excludeId);
        if (!teacherConflicts.isEmpty()) {
            throw new ConflictException("Конфликт расписания: преподаватель уже занят в указанное время");
        }

        List<Schedule> groupConflicts = scheduleRepository.findGroupConflicts(
                request.getDayOfWeek(), request.getWeekNumber(),
                request.getStartTime(), request.getEndTime(),
                request.getGroupId(), excludeId);
        if (!groupConflicts.isEmpty()) {
            throw new ConflictException("Конфликт расписания: у группы уже есть занятие в указанное время");
        }

        List<Schedule> classroomConflicts = scheduleRepository.findClassroomConflicts(
                request.getDayOfWeek(), request.getWeekNumber(),
                request.getStartTime(), request.getEndTime(),
                request.getClassroomId(), excludeId);
        if (!classroomConflicts.isEmpty()) {
            throw new ConflictException("Конфликт расписания: аудитория уже занята в указанное время");
        }
    }

}
