package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.ScheduleCompareRequest;
import org.example.dto.response.*;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.model.schedulecompare.ScheduleCompareSegmentType;
import org.example.model.schedulecompare.ScheduleEntityKind;
import org.example.repository.*;
import org.example.service.ScheduleCompareService;
import org.example.service.UniversityScopeService;
import org.example.service.ViewerUniversityResolver;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCompareServiceImpl implements ScheduleCompareService {

    private final ViewerUniversityResolver viewerUniversityResolver;
    private final UniversityScopeService universityScopeService;
    private final UsersRepository usersRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ScheduleRepository scheduleRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final InstituteRepository instituteRepository;
    private final StudyDirectionRepository studyDirectionRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;

    @Override
    public ScheduleCompareResultResponse compare(ScheduleCompareRequest request, String viewerEmail) {
        Users viewer = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        ScheduleEntityKind leftKind;
        Long leftId;
        final long uni;
        if (request.getMode() == ScheduleCompareRequest.ScheduleCompareMode.MY_WITH_OTHER) {
            if (viewer.getUserType() == UserType.SUPER_ADMIN) {
                throw new AccessDeniedException("Режим «с моим расписанием» недоступен для суперадминистратора");
            }
            uni = viewerUniversityResolver.requireUniversityIdForEmail(viewerEmail);
            if (viewer.getUserType() == UserType.STUDENT) {
                StudentProfile sp = studentProfileRepository.findFetchedByUserId(viewer.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Профиль студента не найден"));
                leftKind = ScheduleEntityKind.GROUP;
                leftId = sp.getGroup().getId();
            } else if (viewer.getUserType() == UserType.TEACHER) {
                leftKind = ScheduleEntityKind.TEACHER;
                leftId = viewer.getId();
            } else {
                throw new AccessDeniedException("Режим «с моим расписанием» доступен студентам и преподавателям");
            }
        } else {
            if (viewer.getUserType() != UserType.ADMIN && viewer.getUserType() != UserType.SUPER_ADMIN) {
                throw new AccessDeniedException("Полное сравнение доступно только администратору");
            }
            if (viewer.getUserType() == UserType.SUPER_ADMIN) {
                if (request.getScopeUniversityId() == null) {
                    throw new BadRequestException("Для суперадминистратора укажите scopeUniversityId (вуз сравнения)");
                }
                uni = request.getScopeUniversityId();
            } else {
                uni = viewerUniversityResolver.requireUniversityIdForEmail(viewerEmail);
            }
            leftKind = request.getLeftKind();
            leftId = request.getLeftId();
            if (leftKind == null || leftId == null) {
                throw new BadRequestException("Укажите левую сторону сравнения (тип и id)");
            }
        }

        ScheduleEntityKind rightKind = request.getRightKind();
        Long rightId = request.getRightId();
        if (rightKind == null || rightId == null) {
            throw new BadRequestException("Укажите правую сторону сравнения");
        }
        if (request.getWeekNumber() == null) {
            throw new BadRequestException("Номер недели обязателен");
        }

        assertEntityInUniversity(leftKind, leftId, uni);
        assertEntityInUniversity(rightKind, rightId, uni);

        log.info("Schedule compare viewer={} mode={} left={}:{} right={}:{} week={} day={}",
                viewerEmail, request.getMode(), leftKind, leftId, rightKind, rightId,
                request.getWeekNumber(), request.getDayOfWeek());

        List<Schedule> leftSched = loadSchedules(leftKind, leftId, request.getWeekNumber(), request.getDayOfWeek());
        List<Schedule> rightSched = loadSchedules(rightKind, rightId, request.getWeekNumber(), request.getDayOfWeek());

        String leftLabel = buildLabel(leftKind, leftId);
        String rightLabel = buildLabel(rightKind, rightId);

        List<Integer> days = request.getDayOfWeek() != null
                ? List.of(request.getDayOfWeek())
                : List.of(1, 2, 3, 4, 5, 6, 7);

        int both = 0;
        int onlyL = 0;
        int onlyR = 0;
        List<ScheduleCompareDayResponse> dayResponses = new ArrayList<>();
        for (int dow : days) {
            List<ScheduleComparisonEngine.RawSegment> raw =
                    ScheduleComparisonEngine.buildRawSegmentsForDay(leftSched, rightSched, dow);
            if (raw.isEmpty()) {
                continue;
            }
            List<ScheduleCompareSegmentResponse> segments = new ArrayList<>();
            for (ScheduleComparisonEngine.RawSegment rs : raw) {
                if (rs.getSegmentType() == ScheduleCompareSegmentType.BOTH) {
                    both++;
                } else if (rs.getSegmentType() == ScheduleCompareSegmentType.ONLY_LEFT) {
                    onlyL++;
                } else {
                    onlyR++;
                }
                segments.add(ScheduleCompareSegmentResponse.builder()
                        .segmentStart(rs.getSegmentStart())
                        .segmentEnd(rs.getSegmentEnd())
                        .segmentType(rs.getSegmentType())
                        .leftEntries(rs.getLeft().stream().map(ScheduleResponseMapper::toResponse).toList())
                        .rightEntries(rs.getRight().stream().map(ScheduleResponseMapper::toResponse).toList())
                        .build());
            }
            dayResponses.add(ScheduleCompareDayResponse.builder()
                    .dayOfWeek(dow)
                    .segments(segments)
                    .build());
        }

        return ScheduleCompareResultResponse.builder()
                .weekNumber(request.getWeekNumber())
                .leftLabel(leftLabel)
                .rightLabel(rightLabel)
                .segmentsBothSidesBusy(both)
                .segmentsOnlyLeft(onlyL)
                .segmentsOnlyRight(onlyR)
                .days(dayResponses)
                .build();
    }

    private void assertEntityInUniversity(ScheduleEntityKind kind, Long id, long uni) {
        switch (kind) {
            case GROUP -> universityScopeService.assertAcademicGroupInUniversity(id, uni);
            case TEACHER -> {
                if (!universityScopeService.teacherUserInUniversity(id, uni)) {
                    throw new AccessDeniedException("Преподаватель не относится к вашему вузу");
                }
            }
            case CLASSROOM -> universityScopeService.assertClassroomInUniversity(id, uni);
        }
    }

    private List<Schedule> loadSchedules(ScheduleEntityKind kind, Long id, Integer week, Integer day) {
        return switch (kind) {
            case GROUP -> findByGroupFiltered(id, week, day);
            case TEACHER -> findByTeacherFiltered(id, week, day);
            case CLASSROOM -> findByClassroomFiltered(id, week, day);
        };
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

    private String buildLabel(ScheduleEntityKind kind, Long id) {
        return switch (kind) {
            case GROUP -> academicGroupRepository.findById(id)
                    .map(g -> "Группа «" + g.getName() + "»")
                    .orElse("Группа");
            case TEACHER -> usersRepository.findById(id)
                    .map(this::formatUserName)
                    .map(n -> "Преподаватель: " + n)
                    .orElse("Преподаватель");
            case CLASSROOM -> classroomRepository.findById(id)
                    .map(c -> "Аудитория: " + c.getBuilding() + ", ауд. " + c.getRoomNumber())
                    .orElse("Аудитория");
        };
    }

    private String formatUserName(Users u) {
        String n = u.getLastName() + " " + u.getFirstName();
        if (u.getMiddleName() != null && !u.getMiddleName().isEmpty()) {
            n += " " + u.getMiddleName();
        }
        return n.trim();
    }

    private long resolveCompareCatalogUniversity(String viewerEmail, Long queryUniversityId) {
        Users viewer = usersRepository.findByEmail(viewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (viewer.getUserType() == UserType.SUPER_ADMIN) {
            if (queryUniversityId == null) {
                throw new BadRequestException("Укажите universityId для каталога сравнения");
            }
            return queryUniversityId;
        }
        return viewerUniversityResolver.requireUniversityIdForEmail(viewerEmail);
    }

    @Override
    public List<ScheduleCompareInstituteOptionResponse> listInstitutes(String viewerEmail, Long universityId) {
        long uni = resolveCompareCatalogUniversity(viewerEmail, universityId);
        log.debug("Schedule compare catalog institutes viewer={} uni={}", viewerEmail, uni);
        return instituteRepository.findByUniversityId(uni).stream()
                .map(i -> ScheduleCompareInstituteOptionResponse.builder()
                        .id(i.getId())
                        .name(i.getName())
                        .shortName(i.getShortName())
                        .build())
                .sorted(Comparator.comparing(ScheduleCompareInstituteOptionResponse::getName, RussianSort::compareText)
                        .thenComparing(ScheduleCompareInstituteOptionResponse::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public List<ScheduleCompareDirectionOptionResponse> listDirections(String viewerEmail, Long universityId,
                                                                       Long instituteId) {
        long uni = resolveCompareCatalogUniversity(viewerEmail, universityId);
        universityScopeService.assertInstituteInUniversity(instituteId, uni);
        return studyDirectionRepository.findByInstituteId(instituteId).stream()
                .map(d -> ScheduleCompareDirectionOptionResponse.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .code(d.getCode())
                        .build())
                .sorted(Comparator.comparing(ScheduleCompareDirectionOptionResponse::getName, RussianSort::compareText)
                        .thenComparing(ScheduleCompareDirectionOptionResponse::getCode, RussianSort::compareText)
                        .thenComparing(ScheduleCompareDirectionOptionResponse::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public List<ScheduleCompareGroupOptionResponse> listGroups(String viewerEmail, Long universityId, Long instituteId,
                                                               Long directionId, String q) {
        long uni = resolveCompareCatalogUniversity(viewerEmail, universityId);
        String qq = (q == null || q.isBlank()) ? null : q.trim();
        if (instituteId != null) {
            universityScopeService.assertInstituteInUniversity(instituteId, uni);
        }
        if (directionId != null) {
            universityScopeService.assertStudyDirectionInUniversity(directionId, uni);
        }
        return academicGroupRepository.searchForCompareCatalog(uni, instituteId, directionId, qq).stream()
                .map(g -> ScheduleCompareGroupOptionResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .directionName(g.getDirection().getName())
                        .instituteName(g.getDirection().getInstitute().getName())
                        .build())
                .sorted(Comparator.comparing(ScheduleCompareGroupOptionResponse::getName, RussianSort::compareText)
                        .thenComparing(ScheduleCompareGroupOptionResponse::getDirectionName, RussianSort::compareText)
                        .thenComparing(ScheduleCompareGroupOptionResponse::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public List<ScheduleCompareTeacherOptionResponse> listTeachers(String viewerEmail, Long universityId, String q) {
        long uni = resolveCompareCatalogUniversity(viewerEmail, universityId);
        Set<Long> ids = new LinkedHashSet<>(teacherProfileRepository.findUserIdsByUniversityId(uni));
        ids.addAll(scheduleRepository.findDistinctTeacherUserIdsByUniversityId(uni));
        String qq = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        List<Users> teachers = usersRepository.findAllById(ids).stream()
                .filter(u -> u.getUserType() == UserType.TEACHER)
                .filter(u -> qq == null || matchesTeacherSearch(u, qq))
                .sorted(RussianSort::compareUsersByName)
                .toList();
        return teachers.stream().map(u -> {
            String inst = null;
            String pos = null;
            var tp = teacherProfileRepository.findByUserId(u.getId());
            if (tp.isPresent()) {
                var profile = tp.get();
                pos = profile.getPosition();
                List<String> instNames = teacherSubjectRepository.findDistinctInstitutesForTeacher(profile.getId()).stream()
                        .map(i -> i.getName())
                        .toList();
                if (!instNames.isEmpty()) {
                    inst = String.join(", ", instNames);
                } else if (profile.getInstitute() != null) {
                    inst = profile.getInstitute().getName();
                }
            }
            return ScheduleCompareTeacherOptionResponse.builder()
                    .userId(u.getId())
                    .displayName(formatUserName(u))
                    .instituteName(inst)
                    .position(pos)
                    .build();
        }).toList();
    }

    private boolean matchesTeacherSearch(Users u, String qq) {
        String blob = (u.getLastName() + " " + u.getFirstName() + " " + Objects.toString(u.getMiddleName(), "")
                + " " + u.getEmail()).toLowerCase(Locale.ROOT);
        return blob.contains(qq);
    }

    @Override
    public List<ScheduleCompareClassroomOptionResponse> listClassrooms(String viewerEmail, Long universityId, String q) {
        long uni = resolveCompareCatalogUniversity(viewerEmail, universityId);
        String qq = (q == null || q.isBlank()) ? null : q.trim().toLowerCase(Locale.ROOT);
        return classroomRepository.findByUniversityId(uni).stream()
                .filter(c -> qq == null || classroomMatches(c, qq))
                .sorted(RussianSort.classroomEntityComparator())
                .map(c -> {
                    String label = c.getBuilding() + ", ауд. " + c.getRoomNumber();
                    return ScheduleCompareClassroomOptionResponse.builder()
                            .id(c.getId())
                            .building(c.getBuilding())
                            .roomNumber(c.getRoomNumber())
                            .capacity(c.getCapacity())
                            .label(label)
                            .build();
                })
                .toList();
    }

    private boolean classroomMatches(Classroom c, String qq) {
        String blob = (Objects.toString(c.getBuilding(), "") + " " + Objects.toString(c.getRoomNumber(), "")
                + " " + Objects.toString(c.getCapacity(), "")).toLowerCase(Locale.ROOT);
        return blob.contains(qq);
    }
}
