package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.*;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.TeacherGradingCatalogService;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherGradingCatalogServiceImpl implements TeacherGradingCatalogService {

    private final UsersRepository usersRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final GradeRepository gradeRepository;
    private final SubjectPracticeRepository subjectPracticeRepository;
    private final PracticeGradeRepository practiceGradeRepository;

    private TeacherProfile requireTeacher(String email) {
        Users u = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (u.getUserType() != UserType.TEACHER) {
            throw new AccessDeniedException("Доступно только преподавателям");
        }
        return teacherProfileRepository.findByUserId(u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Профиль преподавателя не найден"));
    }

    private void assertTeacherTeachesSid(TeacherProfile tp, Long subjectDirectionId) {
        if (!teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(tp.getId(), subjectDirectionId)) {
            throw new AccessDeniedException("Вы не назначены на эту дисциплину в учебном плане");
        }
    }

    @Override
    public List<TeacherGradingPickResponse> listInstitutes(String teacherEmail) {
        TeacherProfile tp = requireTeacher(teacherEmail);
        return teacherSubjectRepository.findDistinctInstitutesForTeacher(tp.getId()).stream()
                .map(i -> TeacherGradingPickResponse.builder()
                        .id(i.getId())
                        .name(i.getName())
                        .subtitle(i.getShortName())
                        .build())
                .sorted(RussianSort.teacherGradingPicks())
                .collect(Collectors.toList());
    }

    @Override
    public List<TeacherGradingPickResponse> listDirections(String teacherEmail, Long instituteId) {
        TeacherProfile tp = requireTeacher(teacherEmail);
        if (instituteId == null) {
            throw new BadRequestException("instituteId обязателен");
        }
        return teacherSubjectRepository.findDistinctDirectionsForTeacherAndInstitute(tp.getId(), instituteId).stream()
                .map(d -> TeacherGradingPickResponse.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .subtitle(d.getCode())
                        .build())
                .sorted(RussianSort.teacherGradingPicks())
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectInDirectionResponse> listSubjectDirections(String teacherEmail, Long directionId) {
        TeacherProfile tp = requireTeacher(teacherEmail);
        if (directionId == null) {
            throw new BadRequestException("directionId обязателен");
        }
        return teacherSubjectRepository.findSubjectDirectionsForTeacherAndDirection(tp.getId(), directionId).stream()
                .map(this::mapSid)
                .sorted(RussianSort.subjectInDirections())
                .collect(Collectors.toList());
    }

    @Override
    public List<TeacherGradingPickResponse> listGroups(String teacherEmail, Long subjectDirectionId) {
        TeacherProfile tp = requireTeacher(teacherEmail);
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция учебного плана не найдена"));
        assertTeacherTeachesSid(tp, subjectDirectionId);
        Long dirId = sid.getDirection().getId();
        return academicGroupRepository.findByDirectionId(dirId).stream()
                .map(g -> TeacherGradingPickResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .subtitle(g.getCourse() != null ? ("Курс " + g.getCourse()) : null)
                        .build())
                .sorted(RussianSort.teacherGradingPicks())
                .collect(Collectors.toList());
    }

    @Override
    public List<TeacherGradingPickResponse> listStudents(String teacherEmail, Long subjectDirectionId, Long groupId) {
        TeacherProfile tp = requireTeacher(teacherEmail);
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция учебного плана не найдена"));
        assertTeacherTeachesSid(tp, subjectDirectionId);
        AcademicGroup group = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
        if (!group.getDirection().getId().equals(sid.getDirection().getId())) {
            throw new BadRequestException("Группа не относится к направлению выбранной дисциплины");
        }
        return studentProfileRepository.findByGroupId(groupId).stream()
                .map(sp -> {
                    Users st = sp.getUser();
                    String name = st.getLastName() + " " + st.getFirstName();
                    if (st.getMiddleName() != null && !st.getMiddleName().isBlank()) {
                        name += " " + st.getMiddleName();
                    }
                    return TeacherGradingPickResponse.builder()
                            .id(st.getId())
                            .name(name)
                            .subtitle(group.getName())
                            .build();
                })
                .sorted(RussianSort.teacherGradingPicks())
                .collect(Collectors.toList());
    }

    @Override
    public TeacherStudentAssessmentResponse getAssessment(String teacherEmail, Long subjectDirectionId,
                                                          Long groupId, Long studentUserId) {
        TeacherProfile tp = requireTeacher(teacherEmail);
        SubjectInDirection sid = subjectInDirectionRepository.findById(subjectDirectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция учебного плана не найдена"));
        assertTeacherTeachesSid(tp, subjectDirectionId);
        AcademicGroup group = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Группа не найдена"));
        if (!group.getDirection().getId().equals(sid.getDirection().getId())) {
            throw new BadRequestException("Группа не относится к направлению выбранной дисциплины");
        }
        Users student = usersRepository.findById(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Студент не найден"));
        if (student.getUserType() != UserType.STUDENT) {
            throw new BadRequestException("Указанный пользователь не студент");
        }
        StudentProfile sp = studentProfileRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль студента не найден"));
        if (!sp.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Студент не из выбранной группы");
        }
        if (!sp.getGroup().getDirection().getId().equals(sid.getDirection().getId())) {
            throw new BadRequestException("Студент не обучается на направлении выбранной дисциплины");
        }

        Institute inst = sid.getDirection().getInstitute();
        String stName = student.getLastName() + " " + student.getFirstName();
        if (student.getMiddleName() != null && !student.getMiddleName().isBlank()) {
            stName += " " + student.getMiddleName();
        }

        FinalAssessmentType fat = sid.getFinalAssessmentType() != null
                ? sid.getFinalAssessmentType()
                : FinalAssessmentType.EXAM;

        GradeResponse finalG = gradeRepository.findByStudentIdAndSubjectDirectionId(studentUserId, subjectDirectionId)
                .map(g -> mapGrade(g, student))
                .orElse(null);

        List<SubjectPractice> practices = new ArrayList<>(
                subjectPracticeRepository.findBySubjectDirectionId(subjectDirectionId));
        RussianSort.sortSubjectPractices(practices);
        List<TeacherPracticeSlotResponse> slots = new ArrayList<>();
        for (SubjectPractice pr : practices) {
            Optional<PracticeGrade> pgOpt = practiceGradeRepository.findByStudentIdAndPracticeId(studentUserId, pr.getId());
            TeacherPracticeSlotResponse.TeacherPracticeSlotResponseBuilder b = TeacherPracticeSlotResponse.builder()
                    .practiceId(pr.getId())
                    .practiceNumber(pr.getPracticeNumber())
                    .practiceTitle(pr.getPracticeTitle())
                    .creditPractice(pr.getIsCredit())
                    .maxGrade(pr.getMaxGrade());
            pgOpt.ifPresent(pg -> b.gradeRowId(pg.getId()).grade(pg.getGrade()).creditStatus(pg.getCreditStatus()));
            slots.add(b.build());
        }

        return TeacherStudentAssessmentResponse.builder()
                .subjectDirectionId(subjectDirectionId)
                .directionId(sid.getDirection().getId())
                .instituteId(inst.getId())
                .groupId(groupId)
                .studentUserId(studentUserId)
                .instituteName(inst.getName())
                .directionName(sid.getDirection().getName())
                .subjectName(sid.getSubject().getName())
                .groupName(group.getName())
                .studentDisplayName(stName)
                .finalAssessmentType(fat.name())
                .subjectInDirection(mapSid(sid))
                .finalGrade(finalG)
                .practices(slots)
                .build();
    }

    private SubjectInDirectionResponse mapSid(SubjectInDirection entity) {
        return SubjectInDirectionResponse.builder()
                .id(entity.getId())
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .directionId(entity.getDirection().getId())
                .directionName(entity.getDirection().getName())
                .course(entity.getCourse())
                .semester(entity.getSemester())
                .finalAssessmentType(entity.getFinalAssessmentType())
                .build();
    }

    private GradeResponse mapGrade(Grade grade, Users student) {
        SubjectInDirection s = grade.getSubjectDirection();
        FinalAssessmentType fat = s.getFinalAssessmentType() != null
                ? s.getFinalAssessmentType()
                : FinalAssessmentType.EXAM;
        String studentName = student.getLastName() + " " + student.getFirstName();
        if (student.getMiddleName() != null && !student.getMiddleName().isEmpty()) {
            studentName += " " + student.getMiddleName();
        }
        return GradeResponse.builder()
                .id(grade.getId())
                .studentId(student.getId())
                .studentName(studentName)
                .subjectDirectionId(s.getId())
                .subjectName(s.getSubject().getName())
                .grade(grade.getGrade())
                .creditStatus(grade.getCreditStatus())
                .finalAssessmentType(fat.name())
                .build();
    }
}
