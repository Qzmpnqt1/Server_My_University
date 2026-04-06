package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.GradeRequest;
import org.example.dto.response.GradeResponse;
import org.example.dto.response.PracticeGradeResponse;
import org.example.dto.response.TeacherJournalResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.AuditService;
import org.example.service.GradeService;
import org.example.service.NotificationService;
import org.example.service.UniversityScopeService;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private static final int MIN_GRADE = 2;
    private static final int MAX_GRADE = 5;

    private final GradeRepository gradeRepository;
    private final PracticeGradeRepository practiceGradeRepository;
    private final SubjectPracticeRepository subjectPracticeRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final UsersRepository usersRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UniversityScopeService universityScopeService;

    @Override
    @Transactional(readOnly = true)
    public List<GradeResponse> getMyGrades(String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (user.getUserType() != UserType.STUDENT) {
            throw new AccessDeniedException("Только студенты могут просматривать свои оценки");
        }

        List<Grade> grades = new ArrayList<>(gradeRepository.findByStudentId(user.getId()));
        grades.sort(RussianSort.gradeEntityBySubjectPlanComparator());
        if (grades.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> practiceCounts = buildPracticeCountMap(
                grades.stream().map(g -> g.getSubjectDirection().getId()).collect(Collectors.toSet()));
        return grades.stream()
                .map(g -> mapToResponse(g, practiceCounts.getOrDefault(g.getSubjectDirection().getId(), 0)))
                .collect(Collectors.toList());
    }

    private Map<Long, Integer> buildPracticeCountMap(Set<Long> subjectDirectionIds) {
        if (subjectDirectionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> out = new HashMap<>();
        for (Long id : subjectDirectionIds) {
            out.put(id, (int) subjectPracticeRepository.countBySubjectDirectionId(id));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeResponse> getByStudent(Long studentId, String email) {
        Users currentUser = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Users student = usersRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        if (student.getUserType() != UserType.STUDENT) {
            throw new BadRequestException("Указанный пользователь не является студентом");
        }

        if (currentUser.getUserType() == UserType.STUDENT) {
            if (!currentUser.getId().equals(studentId)) {
                throw new AccessDeniedException("Студент может просматривать только свои оценки");
            }
        } else if (currentUser.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireCampusUniversityId(email);
            universityScopeService.assertUserInUniversity(studentId, uni);
        } else if (currentUser.getUserType() == UserType.SUPER_ADMIN) {
            // глобальный просмотр
        }

        List<Grade> list = new ArrayList<>(gradeRepository.findByStudentId(studentId));
        list.sort(RussianSort.gradeEntityBySubjectPlanComparator());
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> practiceCounts = buildPracticeCountMap(
                list.stream().map(g -> g.getSubjectDirection().getId()).collect(Collectors.toSet()));
        return list.stream()
                .map(g -> mapToResponse(g, practiceCounts.getOrDefault(g.getSubjectDirection().getId(), 0)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeResponse> getBySubjectDirection(Long subjectDirectionId, String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (user.getUserType() == UserType.STUDENT) {
            throw new AccessDeniedException("Студенты не могут просматривать оценки всех студентов по предмету");
        }

        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));

        if (user.getUserType() == UserType.TEACHER) {
            verifyTeacherOwnership(user, sid);
        } else if (user.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireCampusUniversityId(email);
            universityScopeService.assertSubjectDirectionInUniversity(subjectDirectionId, uni);
        } else if (user.getUserType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Недостаточно прав");
        }

        List<Grade> list = new ArrayList<>(gradeRepository.findBySubjectDirectionId(subjectDirectionId));
        list.sort(RussianSort.gradeEntityByStudentNameComparator());
        int pc = (int) subjectPracticeRepository.countBySubjectDirectionId(subjectDirectionId);
        return list.stream()
                .map(g -> mapToResponse(g, pc))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherJournalResponse getTeacherJournal(Long subjectDirectionId, String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));
        if (user.getUserType() == UserType.TEACHER) {
            verifyTeacherOwnership(user, sid);
        } else if (user.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireCampusUniversityId(email);
            universityScopeService.assertSubjectDirectionInUniversity(subjectDirectionId, uni);
        } else if (user.getUserType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Недостаточно прав для просмотра журнала");
        }

        Long directionId = sid.getDirection().getId();
        List<AcademicGroup> groups = academicGroupRepository.findByDirectionId(directionId);
        Map<Long, StudentProfile> byUser = new LinkedHashMap<>();
        for (AcademicGroup g : groups) {
            for (StudentProfile sp : studentProfileRepository.findByGroupId(g.getId())) {
                byUser.putIfAbsent(sp.getUser().getId(), sp);
            }
        }

        List<SubjectPractice> practices = new ArrayList<>(
                subjectPracticeRepository.findBySubjectDirectionId(subjectDirectionId));
        RussianSort.sortSubjectPractices(practices);
        int practiceCountForGrades = (int) subjectPracticeRepository.countBySubjectDirectionId(subjectDirectionId);
        List<TeacherJournalResponse.StudentRow> rows = new ArrayList<>();
        for (StudentProfile sp : byUser.values()) {
            Users st = sp.getUser();
            String name = st.getLastName() + " " + st.getFirstName();
            if (st.getMiddleName() != null && !st.getMiddleName().isBlank()) {
                name += " " + st.getMiddleName();
            }
            GradeResponse finalG = gradeRepository.findByStudentIdAndSubjectDirectionId(st.getId(), subjectDirectionId)
                    .map(g -> mapToResponse(g, practiceCountForGrades))
                    .orElse(null);
            List<PracticeGradeResponse> pRows = new ArrayList<>();
            for (SubjectPractice pr : practices) {
                Optional<PracticeGrade> optPg = practiceGradeRepository.findByStudentIdAndPracticeId(st.getId(), pr.getId());
                pRows.add(mapPracticeSlot(st, pr, optPg.orElse(null)));
            }
            pRows.sort(Comparator.comparing(PracticeGradeResponse::getPracticeNumber, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(PracticeGradeResponse::getPracticeId, Comparator.nullsLast(Long::compareTo)));
            rows.add(TeacherJournalResponse.StudentRow.builder()
                    .studentUserId(st.getId())
                    .studentName(name)
                    .groupName(sp.getGroup().getName())
                    .finalGrade(finalG)
                    .practiceGrades(pRows)
                    .build());
        }

        rows.sort(Comparator.comparing(TeacherJournalResponse.StudentRow::getStudentName, RussianSort::compareText)
                .thenComparing(TeacherJournalResponse.StudentRow::getStudentUserId, Comparator.nullsLast(Long::compareTo)));

        return TeacherJournalResponse.builder()
                .subjectDirectionId(subjectDirectionId)
                .subjectName(sid.getSubject().getName())
                .students(rows)
                .build();
    }

    @Override
    @Transactional
    public GradeResponse create(GradeRequest request, String email) {
        Users currentUser = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Users student = usersRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        if (student.getUserType() != UserType.STUDENT) {
            throw new BadRequestException("Указанный пользователь не является студентом");
        }

        SubjectInDirection sid = subjectInDirectionRepository.findById(request.getSubjectDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));

        if (currentUser.getUserType() == UserType.TEACHER) {
            verifyTeacherOwnership(currentUser, sid);
        } else if (currentUser.getUserType() == UserType.ADMIN
                || currentUser.getUserType() == UserType.SUPER_ADMIN) {
            Long uni = sid.getDirection().getInstitute().getUniversity().getId();
            universityScopeService.enforceAccessToEntityUniversity(email, uni);
            universityScopeService.assertSubjectDirectionInUniversity(request.getSubjectDirectionId(), uni);
            universityScopeService.assertUserInUniversity(request.getStudentId(), uni);
        } else {
            throw new AccessDeniedException("Недостаточно прав");
        }

        assertStudentEligibleForSubjectDirection(student, sid, request.getGroupId());

        validateGrade(request, sid);

        gradeRepository.findByStudentIdAndSubjectDirectionId(request.getStudentId(), request.getSubjectDirectionId())
                .ifPresent(g -> {
                    throw new ConflictException("Оценка для данного студента по данному предмету уже существует");
                });

        Grade grade = Grade.builder()
                .student(student)
                .subjectDirection(sid)
                .grade(request.getGrade())
                .creditStatus(request.getCreditStatus())
                .build();

        grade = gradeRepository.save(grade);

        auditService.log(currentUser.getId(), "CREATE_GRADE", "Grade", grade.getId(),
                "Выставлена оценка студенту " + student.getEmail() + " по предмету " + sid.getSubject().getName());
        notificationService.notifyGradeChanged(student.getId(), sid.getSubject().getName(), false);

        return mapToResponse(grade);
    }

    @Override
    @Transactional
    public GradeResponse update(Long id, GradeRequest request, String email) {
        Users currentUser = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Оценка не найдена"));

        if (currentUser.getUserType() == UserType.TEACHER) {
            verifyTeacherOwnership(currentUser, grade.getSubjectDirection());
        } else if (currentUser.getUserType() == UserType.ADMIN
                || currentUser.getUserType() == UserType.SUPER_ADMIN) {
            SubjectInDirection sid = grade.getSubjectDirection();
            Long uni = sid.getDirection().getInstitute().getUniversity().getId();
            universityScopeService.enforceAccessToEntityUniversity(email, uni);
            universityScopeService.assertUserInUniversity(grade.getStudent().getId(), uni);
            universityScopeService.assertSubjectDirectionInUniversity(grade.getSubjectDirection().getId(), uni);
        } else {
            throw new AccessDeniedException("Недостаточно прав");
        }

        assertStudentEligibleForSubjectDirection(grade.getStudent(), grade.getSubjectDirection(), request.getGroupId());

        validateGrade(request, grade.getSubjectDirection());

        grade.setGrade(request.getGrade());
        grade.setCreditStatus(request.getCreditStatus());

        grade = gradeRepository.save(grade);

        auditService.log(currentUser.getId(), "UPDATE_GRADE", "Grade", grade.getId(),
                "Обновлена оценка студенту " + grade.getStudent().getEmail());
        notificationService.notifyGradeChanged(grade.getStudent().getId(),
                grade.getSubjectDirection().getSubject().getName(), false);

        return mapToResponse(grade);
    }

    private PracticeGradeResponse mapPracticeSlot(Users student, SubjectPractice pr, PracticeGrade pg) {
        String sn = student.getLastName() + " " + student.getFirstName();
        if (pg != null) {
            return PracticeGradeResponse.builder()
                    .id(pg.getId())
                    .studentId(student.getId())
                    .studentName(sn)
                    .practiceId(pr.getId())
                    .practiceTitle(pr.getPracticeTitle())
                    .practiceNumber(pr.getPracticeNumber())
                    .grade(pg.getGrade())
                    .creditStatus(pg.getCreditStatus())
                    .maxGrade(pr.getMaxGrade())
                    .practiceIsCredit(pr.getIsCredit())
                    .build();
        }
        return PracticeGradeResponse.builder()
                .studentId(student.getId())
                .studentName(sn)
                .practiceId(pr.getId())
                .practiceTitle(pr.getPracticeTitle())
                .practiceNumber(pr.getPracticeNumber())
                .maxGrade(pr.getMaxGrade())
                .practiceIsCredit(pr.getIsCredit())
                .build();
    }

    private void validateGrade(GradeRequest request, SubjectInDirection sid) {
        FinalAssessmentType type = sid.getFinalAssessmentType() != null
                ? sid.getFinalAssessmentType()
                : FinalAssessmentType.EXAM;
        if (type == FinalAssessmentType.CREDIT) {
            if (request.getGrade() != null) {
                throw new BadRequestException("Для зачётного предмета укажите только creditStatus, без числовой оценки");
            }
            if (request.getCreditStatus() == null) {
                throw new BadRequestException("Для зачётного предмета необходим creditStatus (зачёт/незачёт)");
            }
        } else {
            if (request.getCreditStatus() != null) {
                throw new BadRequestException("Для экзаменационного предмета укажите только grade (2–5), без creditStatus");
            }
            if (request.getGrade() == null) {
                throw new BadRequestException("Для экзаменационного предмета необходима числовая оценка 2–5");
            }
            if (request.getGrade() < MIN_GRADE || request.getGrade() > MAX_GRADE) {
                throw new BadRequestException(
                        "Итоговая оценка по экзамену должна быть от " + MIN_GRADE + " до " + MAX_GRADE);
            }
        }
    }

    private void assertStudentEligibleForSubjectDirection(Users student, SubjectInDirection sid, Long groupId) {
        StudentProfile sp = studentProfileRepository.findFetchedByUserId(student.getId())
                .orElseThrow(() -> new BadRequestException("Профиль студента не найден"));
        if (!sp.getGroup().getDirection().getId().equals(sid.getDirection().getId())) {
            throw new BadRequestException("Студент не обучается на направлении выбранной дисциплины");
        }
        if (groupId != null && !sp.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Студент не из выбранной группы");
        }
    }

    private void verifyTeacherOwnership(Users user, SubjectInDirection sid) {
        if (user.getUserType() != UserType.TEACHER) {
            throw new AccessDeniedException("Только преподаватели и администраторы могут управлять оценками");
        }

        TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профиль преподавателя не найден"));

        boolean assigned = teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(
                teacherProfile.getId(), sid.getId());
        if (!assigned) {
            throw new AccessDeniedException("Преподаватель не назначен на данный предмет");
        }
    }

    private GradeResponse mapToResponse(Grade grade) {
        int pc = (int) subjectPracticeRepository.countBySubjectDirectionId(grade.getSubjectDirection().getId());
        return mapToResponse(grade, pc);
    }

    private GradeResponse mapToResponse(Grade grade, int practiceCount) {
        Users student = grade.getStudent();
        SubjectInDirection sid = grade.getSubjectDirection();

        String studentName = student.getLastName() + " " + student.getFirstName();
        if (student.getMiddleName() != null && !student.getMiddleName().isEmpty()) {
            studentName += " " + student.getMiddleName();
        }

        FinalAssessmentType fat = sid.getFinalAssessmentType() != null
                ? sid.getFinalAssessmentType()
                : FinalAssessmentType.EXAM;
        StudyDirection direction = sid.getDirection();
        String directionName = direction != null ? direction.getName() : null;

        return GradeResponse.builder()
                .id(grade.getId())
                .studentId(student.getId())
                .studentName(studentName)
                .subjectDirectionId(sid.getId())
                .subjectName(sid.getSubject().getName())
                .grade(grade.getGrade())
                .creditStatus(grade.getCreditStatus())
                .finalAssessmentType(fat.name())
                .course(sid.getCourse())
                .semester(sid.getSemester())
                .directionName(directionName)
                .practiceCount(practiceCount)
                .build();
    }
}
