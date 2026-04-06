package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.PracticeGradeRequest;
import org.example.dto.response.PracticeGradeResponse;
import org.example.dto.response.StudentPracticeSlotResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.AuditService;
import org.example.service.NotificationService;
import org.example.service.PracticeGradeService;
import org.example.service.UniversityScopeService;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeGradeServiceImpl implements PracticeGradeService {

    private final PracticeGradeRepository practiceGradeRepository;
    private final SubjectPracticeRepository subjectPracticeRepository;
    private final UsersRepository usersRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<PracticeGradeResponse> getMyPracticeGrades(String email, Long subjectDirectionId) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (user.getUserType() != UserType.STUDENT) {
            throw new AccessDeniedException("Только студенты могут просматривать свои оценки за практики");
        }

        if (subjectDirectionId != null) {
            subjectInDirectionRepository.findById(subjectDirectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));

            List<SubjectPractice> practices = subjectPracticeRepository.findBySubjectDirectionId(subjectDirectionId);
            List<Long> practiceIds = practices.stream()
                    .map(SubjectPractice::getId)
                    .collect(Collectors.toList());

            if (practiceIds.isEmpty()) {
                return List.of();
            }

            List<PracticeGradeResponse> pgList = practiceGradeRepository.findByStudentIdAndPracticeIdIn(user.getId(), practiceIds).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            pgList.sort(Comparator.comparing(PracticeGradeResponse::getPracticeNumber, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(PracticeGradeResponse::getPracticeId, Comparator.nullsLast(Long::compareTo)));
            return pgList;
        }

        List<PracticeGradeResponse> allPg = practiceGradeRepository.findByStudentId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        allPg.sort(Comparator.comparing(PracticeGradeResponse::getPracticeTitle, RussianSort::compareText)
                .thenComparing(PracticeGradeResponse::getPracticeNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PracticeGradeResponse::getId, Comparator.nullsLast(Long::compareTo)));
        return allPg;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentPracticeSlotResponse> getMyPracticeSlotsForSubject(String email, Long subjectDirectionId) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getUserType() != UserType.STUDENT) {
            throw new AccessDeniedException("Только студенты могут просматривать слоты практик");
        }
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет в направлении не найден"));
        assertStudentOwnsSubjectDirection(user, sid);

        List<SubjectPractice> practices = new ArrayList<>(
                subjectPracticeRepository.findBySubjectDirectionId(subjectDirectionId));
        practices.sort(Comparator.comparing(SubjectPractice::getPracticeNumber,
                Comparator.nullsLast(Comparator.naturalOrder())));

        if (practices.isEmpty()) {
            return List.of();
        }
        List<Long> practiceIds = practices.stream().map(SubjectPractice::getId).collect(Collectors.toList());
        List<PracticeGrade> existing = practiceGradeRepository.findByStudentIdAndPracticeIdIn(
                user.getId(), practiceIds);
        Map<Long, PracticeGrade> byPracticeId = existing.stream()
                .collect(Collectors.toMap(pg -> pg.getPractice().getId(), pg -> pg, (a, b) -> a));

        List<StudentPracticeSlotResponse> out = new ArrayList<>(practices.size());
        for (SubjectPractice pr : practices) {
            PracticeGrade pg = byPracticeId.get(pr.getId());
            out.add(mapStudentSlot(pr, pg));
        }
        return out;
    }

    private void assertStudentOwnsSubjectDirection(Users student, SubjectInDirection sid) {
        StudentProfile sp = studentProfileRepository.findFetchedByUserId(student.getId())
                .orElseThrow(() -> new BadRequestException("Профиль студента не найден"));
        if (!sp.getGroup().getDirection().getId().equals(sid.getDirection().getId())) {
            throw new AccessDeniedException("Нет доступа к практикам этой дисциплины");
        }
    }

    private StudentPracticeSlotResponse mapStudentSlot(SubjectPractice pr, PracticeGrade pg) {
        Integer grade = pg != null ? pg.getGrade() : null;
        Boolean creditStatus = pg != null ? pg.getCreditStatus() : null;
        boolean hasResult = computePracticeHasResult(pr, pg);
        return StudentPracticeSlotResponse.builder()
                .practiceId(pr.getId())
                .practiceNumber(pr.getPracticeNumber())
                .practiceTitle(pr.getPracticeTitle())
                .maxGrade(pr.getMaxGrade())
                .isCredit(pr.getIsCredit())
                .grade(grade)
                .creditStatus(creditStatus)
                .hasResult(hasResult)
                .build();
    }

    private boolean computePracticeHasResult(SubjectPractice pr, PracticeGrade pg) {
        if (pg == null) {
            return false;
        }
        if (Boolean.TRUE.equals(pr.getIsCredit())) {
            return pg.getCreditStatus() != null;
        }
        if (pg.getGrade() == null) {
            return false;
        }
        int g = pg.getGrade();
        Integer max = pr.getMaxGrade();
        if (max != null) {
            return g >= 0 && g <= max;
        }
        return g >= 2 && g <= 5;
    }

    @Override
    public List<PracticeGradeResponse> getByPractice(Long practiceId, String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (user.getUserType() == UserType.STUDENT) {
            throw new AccessDeniedException("Студенты не могут просматривать оценки всех студентов за практику");
        }

        SubjectPractice practice = subjectPracticeRepository.findById(practiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Практическая работа не найдена"));

        if (user.getUserType() == UserType.TEACHER) {
            verifyTeacherOwnership(user, practice.getSubjectDirection());
        } else if (user.getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireCampusUniversityId(email);
            universityScopeService.assertSubjectDirectionInUniversity(practice.getSubjectDirection().getId(), uni);
        } else if (user.getUserType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("Недостаточно прав");
        }

        List<PracticeGradeResponse> out = practiceGradeRepository.findByPracticeId(practiceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        out.sort(Comparator.comparing(PracticeGradeResponse::getStudentName, RussianSort::compareText)
                .thenComparing(PracticeGradeResponse::getStudentId, Comparator.nullsLast(Long::compareTo)));
        return out;
    }

    @Override
    @Transactional
    public PracticeGradeResponse create(PracticeGradeRequest request, String email) {
        Users currentUser = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Users student = usersRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        if (student.getUserType() != UserType.STUDENT) {
            throw new BadRequestException("Указанный пользователь не является студентом");
        }

        SubjectPractice practice = subjectPracticeRepository.findById(request.getPracticeId())
                .orElseThrow(() -> new ResourceNotFoundException("Практическая работа не найдена"));

        if (currentUser.getUserType() == UserType.TEACHER) {
            verifyTeacherOwnership(currentUser, practice.getSubjectDirection());
        } else if (currentUser.getUserType() == UserType.ADMIN
                || currentUser.getUserType() == UserType.SUPER_ADMIN) {
            SubjectInDirection sid = practice.getSubjectDirection();
            Long uni = sid.getDirection().getInstitute().getUniversity().getId();
            universityScopeService.enforceAccessToEntityUniversity(email, uni);
            universityScopeService.assertUserInUniversity(request.getStudentId(), uni);
            universityScopeService.assertSubjectDirectionInUniversity(practice.getSubjectDirection().getId(), uni);
        } else {
            throw new AccessDeniedException("Недостаточно прав");
        }

        assertStudentEligibleForPractice(student, practice, request.getGroupId());

        validateGrade(request, practice);

        practiceGradeRepository.findByStudentIdAndPracticeId(request.getStudentId(), request.getPracticeId())
                .ifPresent(pg -> {
                    throw new ConflictException("Оценка за данную практику для данного студента уже существует");
                });

        PracticeGrade practiceGrade = PracticeGrade.builder()
                .student(student)
                .practice(practice)
                .grade(request.getGrade())
                .creditStatus(request.getCreditStatus())
                .build();

        practiceGrade = practiceGradeRepository.save(practiceGrade);

        auditService.log(currentUser.getId(), "CREATE_PRACTICE_GRADE", "PracticeGrade", practiceGrade.getId(),
                "Выставлена оценка за практику \"" + practice.getPracticeTitle() + "\" студенту " + student.getEmail());
        notificationService.notifyGradeChanged(student.getId(),
                practice.getSubjectDirection().getSubject().getName(), true);

        return mapToResponse(practiceGrade);
    }

    @Override
    @Transactional
    public PracticeGradeResponse update(Long id, PracticeGradeRequest request, String email) {
        Users currentUser = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        PracticeGrade practiceGrade = practiceGradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Оценка за практику не найдена"));

        if (currentUser.getUserType() == UserType.TEACHER) {
            verifyTeacherOwnership(currentUser, practiceGrade.getPractice().getSubjectDirection());
        } else if (currentUser.getUserType() == UserType.ADMIN
                || currentUser.getUserType() == UserType.SUPER_ADMIN) {
            SubjectInDirection sid = practiceGrade.getPractice().getSubjectDirection();
            Long uni = sid.getDirection().getInstitute().getUniversity().getId();
            universityScopeService.enforceAccessToEntityUniversity(email, uni);
            universityScopeService.assertUserInUniversity(practiceGrade.getStudent().getId(), uni);
            universityScopeService.assertSubjectDirectionInUniversity(
                    practiceGrade.getPractice().getSubjectDirection().getId(), uni);
        } else {
            throw new AccessDeniedException("Недостаточно прав");
        }

        assertStudentEligibleForPractice(practiceGrade.getStudent(), practiceGrade.getPractice(), request.getGroupId());

        validateGrade(request, practiceGrade.getPractice());

        practiceGrade.setGrade(request.getGrade());
        practiceGrade.setCreditStatus(request.getCreditStatus());

        practiceGrade = practiceGradeRepository.save(practiceGrade);

        auditService.log(currentUser.getId(), "UPDATE_PRACTICE_GRADE", "PracticeGrade", practiceGrade.getId(),
                "Обновлена оценка за практику студенту " + practiceGrade.getStudent().getEmail());
        notificationService.notifyGradeChanged(practiceGrade.getStudent().getId(),
                practiceGrade.getPractice().getSubjectDirection().getSubject().getName(), true);

        return mapToResponse(practiceGrade);
    }

    private void assertStudentEligibleForPractice(Users student, SubjectPractice practice, Long groupId) {
        SubjectInDirection sid = practice.getSubjectDirection();
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
            throw new AccessDeniedException("Только преподаватели и администраторы могут управлять оценками за практики");
        }

        TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профиль преподавателя не найден"));

        boolean assigned = teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(
                teacherProfile.getId(), sid.getId());
        if (!assigned) {
            throw new AccessDeniedException("Преподаватель не назначен на данный предмет");
        }
    }

    private void validateGrade(PracticeGradeRequest request, SubjectPractice practice) {
        if (Boolean.TRUE.equals(practice.getIsCredit())) {
            if (request.getGrade() != null) {
                throw new BadRequestException("Для зачётной практики укажите только creditStatus");
            }
            if (request.getCreditStatus() == null) {
                throw new BadRequestException("Для зачётной практики необходим creditStatus");
            }
        } else {
            if (request.getCreditStatus() != null) {
                throw new BadRequestException("Для оценочной практики укажите только grade, без creditStatus");
            }
            if (request.getGrade() == null) {
                throw new BadRequestException("Для оценочной практики необходима числовая оценка");
            }
            int g = request.getGrade();
            Integer max = practice.getMaxGrade();
            if (max != null) {
                if (g < 0 || g > max) {
                    throw new BadRequestException("Оценка должна быть от 0 до " + max);
                }
            } else {
                if (g < 2 || g > 5) {
                    throw new BadRequestException("Оценка за оценочную практику должна быть от 2 до 5");
                }
            }
        }
    }

    private PracticeGradeResponse mapToResponse(PracticeGrade pg) {
        Users student = pg.getStudent();
        SubjectPractice practice = pg.getPractice();

        String studentName = student.getLastName() + " " + student.getFirstName();
        if (student.getMiddleName() != null && !student.getMiddleName().isEmpty()) {
            studentName += " " + student.getMiddleName();
        }

        return PracticeGradeResponse.builder()
                .id(pg.getId())
                .studentId(student.getId())
                .studentName(studentName)
                .practiceId(practice.getId())
                .practiceTitle(practice.getPracticeTitle())
                .practiceNumber(practice.getPracticeNumber())
                .grade(pg.getGrade())
                .creditStatus(pg.getCreditStatus())
                .maxGrade(practice.getMaxGrade())
                .practiceIsCredit(practice.getIsCredit())
                .build();
    }
}
