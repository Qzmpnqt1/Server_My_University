package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.PracticeGradeRequest;
import org.example.dto.response.PracticeGradeResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

            return practiceGradeRepository.findByStudentIdAndPracticeIdIn(user.getId(), practiceIds).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return practiceGradeRepository.findByStudentId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
            Long uni = universityScopeService.requireAdminUniversityId(email);
            universityScopeService.assertSubjectDirectionInUniversity(practice.getSubjectDirection().getId(), uni);
        }

        return practiceGradeRepository.findByPracticeId(practiceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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

        if (currentUser.getUserType() != UserType.ADMIN) {
            verifyTeacherOwnership(currentUser, practice.getSubjectDirection());
        } else {
            Long uni = universityScopeService.requireAdminUniversityId(email);
            universityScopeService.assertUserInUniversity(request.getStudentId(), uni);
            universityScopeService.assertSubjectDirectionInUniversity(practice.getSubjectDirection().getId(), uni);
        }

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

        if (currentUser.getUserType() != UserType.ADMIN) {
            verifyTeacherOwnership(currentUser, practiceGrade.getPractice().getSubjectDirection());
        } else {
            Long uni = universityScopeService.requireAdminUniversityId(email);
            universityScopeService.assertUserInUniversity(practiceGrade.getStudent().getId(), uni);
            universityScopeService.assertSubjectDirectionInUniversity(
                    practiceGrade.getPractice().getSubjectDirection().getId(), uni);
        }

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

    private void verifyTeacherOwnership(Users user, SubjectInDirection sid) {
        if (user.getUserType() != UserType.TEACHER) {
            throw new AccessDeniedException("Только преподаватели и администраторы могут управлять оценками за практики");
        }

        TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профиль преподавателя не найден"));

        boolean assigned = teacherSubjectRepository.existsByTeacherIdAndSubjectId(
                teacherProfile.getId(), sid.getSubject().getId());
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
                throw new BadRequestException("Для оценочной практики укажите только grade (2–5), без creditStatus");
            }
            if (request.getGrade() == null) {
                throw new BadRequestException("Для оценочной практики необходима числовая оценка 2–5");
            }
            int g = request.getGrade();
            if (g < 2 || g > 5) {
                throw new BadRequestException("Оценка за оценочную практику должна быть от 2 до 5");
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
