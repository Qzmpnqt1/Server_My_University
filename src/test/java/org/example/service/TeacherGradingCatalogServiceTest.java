package org.example.service;

import org.example.dto.response.TeacherGradingPickResponse;
import org.example.dto.response.TeacherStudentAssessmentResponse;
import org.example.exception.BadRequestException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.impl.TeacherGradingCatalogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherGradingCatalogServiceTest {

    @Mock private UsersRepository usersRepository;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private SubjectInDirectionRepository subjectInDirectionRepository;
    @Mock private AcademicGroupRepository academicGroupRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private SubjectPracticeRepository subjectPracticeRepository;
    @Mock private PracticeGradeRepository practiceGradeRepository;

    @InjectMocks
    private TeacherGradingCatalogServiceImpl catalogService;

    private Users teacherUser;
    private TeacherProfile teacherProfile;
    private StudyDirection direction;
    private Subject subject;
    private SubjectInDirection sidCourse1;

    @BeforeEach
    void setUp() {
        University uni = University.builder().id(1L).name("U").build();
        Institute inst = Institute.builder().id(1L).name("I").university(uni).build();
        direction = StudyDirection.builder().id(1L).name("ИВТ").institute(inst).build();
        subject = Subject.builder().id(1L).name("Математика").build();
        sidCourse1 = SubjectInDirection.builder()
                .id(10L).subject(subject).direction(direction).course(1).semester(1).build();

        teacherUser = Users.builder()
                .id(2L).email("t@test.ru").userType(UserType.TEACHER).build();
        teacherProfile = TeacherProfile.builder().id(20L).user(teacherUser).build();

        when(usersRepository.findByEmail("t@test.ru")).thenReturn(Optional.of(teacherUser));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        lenient().when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(20L, 10L)).thenReturn(true);
        lenient().when(subjectInDirectionRepository.findById(10L)).thenReturn(Optional.of(sidCourse1));
    }

    @Test
    @DisplayName("listGroups: только группы с тем же курсом, что и дисциплина")
    void listGroupsFiltersByCourse() {
        AcademicGroup g1 = AcademicGroup.builder().id(1L).name("ИТ-101").course(1).direction(direction).build();
        AcademicGroup g2 = AcademicGroup.builder().id(2L).name("ИТ-201").course(2).direction(direction).build();
        when(academicGroupRepository.findByDirectionId(1L)).thenReturn(List.of(g1, g2));

        List<TeacherGradingPickResponse> picks = catalogService.listGroups("t@test.ru", 10L);

        assertEquals(1, picks.size());
        assertEquals(1L, picks.get(0).getId());
        assertEquals("ИТ-101", picks.get(0).getName());
    }

    @Test
    @DisplayName("listStudents: группа другого курса — BadRequestException")
    void listStudentsRejectsWrongCourseGroup() {
        AcademicGroup wrong = AcademicGroup.builder().id(99L).name("ИТ-301").course(3).direction(direction).build();
        when(academicGroupRepository.findById(99L)).thenReturn(Optional.of(wrong));

        assertThrows(BadRequestException.class,
                () -> catalogService.listStudents("t@test.ru", 10L, 99L));
        verify(studentProfileRepository, never()).findByGroupId(anyLong());
    }

    @Test
    @DisplayName("listInstitutes: возвращает институты преподавателя")
    void listInstitutes_ok() {
        Institute inst = Institute.builder().id(5L).name("Институт").shortName("Ин").build();
        when(teacherSubjectRepository.findDistinctInstitutesForTeacher(20L)).thenReturn(List.of(inst));

        List<TeacherGradingPickResponse> picks = catalogService.listInstitutes("t@test.ru");

        assertEquals(1, picks.size());
        assertEquals(5L, picks.get(0).getId());
        assertEquals("Институт", picks.get(0).getName());
    }

    @Test
    @DisplayName("listDirections: без instituteId — BadRequestException")
    void listDirections_nullInstitute() {
        assertThrows(BadRequestException.class, () -> catalogService.listDirections("t@test.ru", null));
    }

    @Test
    @DisplayName("listSubjectDirections: без directionId — BadRequestException")
    void listSubjectDirections_nullDirection() {
        assertThrows(BadRequestException.class, () -> catalogService.listSubjectDirections("t@test.ru", null));
    }

    @Test
    @DisplayName("listStudents: успешный список студентов группы")
    void listStudents_ok() {
        AcademicGroup g1 = AcademicGroup.builder().id(1L).name("ИТ-101").course(1).direction(direction).build();
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(g1));

        Users st = Users.builder().id(30L).email("st@t.ru").firstName("Иван").lastName("Иванов")
                .userType(UserType.STUDENT).build();
        StudentProfile sp = StudentProfile.builder().id(300L).user(st).group(g1).build();
        when(studentProfileRepository.findByGroupId(1L)).thenReturn(List.of(sp));

        List<TeacherGradingPickResponse> picks = catalogService.listStudents("t@test.ru", 10L, 1L);

        assertEquals(1, picks.size());
        assertEquals(30L, picks.get(0).getId());
        assertTrue(picks.get(0).getName().contains("Иванов"));
    }

    @Test
    @DisplayName("getAssessment: базовый сценарий без оценок и практик")
    void getAssessment_minimal() {
        AcademicGroup g1 = AcademicGroup.builder().id(1L).name("ИТ-101").course(1).direction(direction).build();
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(g1));

        Users st = Users.builder().id(30L).email("st@t.ru").firstName("Пётр").lastName("Петров")
                .userType(UserType.STUDENT).build();
        StudentProfile sp = StudentProfile.builder().id(300L).user(st).group(g1).build();
        when(usersRepository.findById(30L)).thenReturn(Optional.of(st));
        when(studentProfileRepository.findByUserId(30L)).thenReturn(Optional.of(sp));
        when(gradeRepository.findByStudentIdAndSubjectDirectionId(30L, 10L)).thenReturn(Optional.empty());
        when(subjectPracticeRepository.findBySubjectDirectionId(10L)).thenReturn(List.of());

        TeacherStudentAssessmentResponse res = catalogService.getAssessment("t@test.ru", 10L, 1L, 30L);

        assertEquals(10L, res.getSubjectDirectionId());
        assertEquals(30L, res.getStudentUserId());
        assertEquals("Петров Пётр", res.getStudentDisplayName());
        assertTrue(res.getPractices().isEmpty());
    }
}
