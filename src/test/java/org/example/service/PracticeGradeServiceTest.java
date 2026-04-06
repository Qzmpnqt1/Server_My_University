package org.example.service;

import org.example.dto.request.PracticeGradeRequest;
import org.example.dto.response.PracticeGradeResponse;
import org.example.dto.response.StudentPracticeSlotResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.UniversityScopeService;
import org.example.service.impl.PracticeGradeServiceImpl;
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
class PracticeGradeServiceTest {

    @Mock private PracticeGradeRepository practiceGradeRepository;
    @Mock private SubjectPracticeRepository subjectPracticeRepository;
    @Mock private UsersRepository usersRepository;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private SubjectInDirectionRepository subjectInDirectionRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private UniversityScopeService universityScopeService;

    @InjectMocks
    private PracticeGradeServiceImpl practiceGradeService;

    private Users teacherUser;
    private Users studentUser;
    private Users adminUser;
    private TeacherProfile teacherProfile;
    private Subject subject;
    private SubjectInDirection subjectInDirection;
    private SubjectPractice practice;
    private SubjectPractice creditPractice;
    private StudyDirection studyDirection;
    private AcademicGroup studentGroup;
    private StudentProfile studentProfile;

    @BeforeEach
    void setUp() {
        subject = Subject.builder().id(1L).name("Программирование").build();
        studyDirection = StudyDirection.builder().id(1L).name("ИВТ").build();
        subjectInDirection = SubjectInDirection.builder()
                .id(1L).subject(subject).direction(studyDirection).semester(1).course(1).build();

        practice = SubjectPractice.builder()
                .id(1L).subjectDirection(subjectInDirection).practiceNumber(1)
                .practiceTitle("Лабораторная 1").maxGrade(10).isCredit(false).build();

        creditPractice = SubjectPractice.builder()
                .id(2L).subjectDirection(subjectInDirection).practiceNumber(2)
                .practiceTitle("Зачётная работа").isCredit(true).maxGrade(null).build();

        teacherUser = Users.builder()
                .id(2L).email("teacher@test.ru").firstName("Алексей").lastName("Попов")
                .userType(UserType.TEACHER).isActive(true).build();

        studentUser = Users.builder()
                .id(3L).email("student@test.ru").firstName("Иван").lastName("Иванов")
                .userType(UserType.STUDENT).isActive(true).build();

        adminUser = Users.builder()
                .id(4L).email("admin@test.ru").firstName("Админ").lastName("Админов")
                .userType(UserType.ADMIN).isActive(true).build();

        teacherProfile = TeacherProfile.builder().id(10L).user(teacherUser).build();
        studentGroup = AcademicGroup.builder().id(200L).name("ПИ-21").direction(studyDirection).build();
        studentProfile = StudentProfile.builder().id(60L).user(studentUser).group(studentGroup).build();
        lenient().when(studentProfileRepository.findFetchedByUserId(3L)).thenReturn(Optional.of(studentProfile));

        lenient().doNothing().when(notificationService).notifyGradeChanged(anyLong(), anyString(), anyBoolean());
        lenient().when(universityScopeService.requireCampusUniversityId("admin@test.ru")).thenReturn(1L);
        lenient().doNothing().when(universityScopeService).requireAdminOrSuperAdmin("admin@test.ru");
        lenient().doNothing().when(universityScopeService).assertUserInUniversity(anyLong(), eq(1L));
        lenient().doNothing().when(universityScopeService).assertSubjectDirectionInUniversity(anyLong(), eq(1L));
    }

    private PracticeGrade samplePracticeGrade() {
        return PracticeGrade.builder()
                .id(1L).student(studentUser).practice(practice).grade(5).build();
    }

    // ── getMyPracticeGrades ──────────────────────────────────────

    @Test
    @DisplayName("Student can view own practice grades")
    void getMyPracticeGradesStudent() {
        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(studentUser));
        when(practiceGradeRepository.findByStudentId(3L)).thenReturn(List.of(samplePracticeGrade()));

        List<PracticeGradeResponse> result =
                practiceGradeService.getMyPracticeGrades("student@test.ru", null);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getGrade());
        assertEquals("Лабораторная 1", result.get(0).getPracticeTitle());
    }

    @Test
    @DisplayName("Student can filter by subjectDirectionId")
    void getMyPracticeGradesFiltered() {
        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(subjectPracticeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(practice));
        when(practiceGradeRepository.findByStudentIdAndPracticeIdIn(eq(3L), anyList()))
                .thenReturn(List.of(samplePracticeGrade()));

        List<PracticeGradeResponse> result =
                practiceGradeService.getMyPracticeGrades("student@test.ru", 1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Non-student cannot use getMyPracticeGrades")
    void getMyPracticeGradesNonStudent() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        assertThrows(AccessDeniedException.class,
                () -> practiceGradeService.getMyPracticeGrades("teacher@test.ru", null));
    }

    // ── getMyPracticeSlotsForSubject ─────────────────────────────

    @Test
    @DisplayName("Student receives full practice slots for own direction subject")
    void getMyPracticeSlots_ok() {
        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(subjectPracticeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(practice, creditPractice));
        when(practiceGradeRepository.findByStudentIdAndPracticeIdIn(eq(3L), anyList()))
                .thenReturn(List.of(samplePracticeGrade()));

        List<StudentPracticeSlotResponse> slots =
                practiceGradeService.getMyPracticeSlotsForSubject("student@test.ru", 1L);

        assertEquals(2, slots.size());
        assertEquals(1L, slots.get(0).getPracticeId());
        assertTrue(slots.get(0).isHasResult());
        assertEquals(2L, slots.get(1).getPracticeId());
        assertFalse(slots.get(1).isHasResult());
    }

    @Test
    @DisplayName("Student cannot load slots for subject from another direction")
    void getMyPracticeSlots_wrongDirection() {
        StudyDirection otherDir = StudyDirection.builder().id(99L).name("Другое").build();
        SubjectInDirection otherSid = SubjectInDirection.builder()
                .id(2L).subject(subject).direction(otherDir).course(1).semester(1).build();
        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(2L)).thenReturn(Optional.of(otherSid));

        assertThrows(AccessDeniedException.class,
                () -> practiceGradeService.getMyPracticeSlotsForSubject("student@test.ru", 2L));
    }

    // ── getByPractice ────────────────────────────────────────────

    @Test
    @DisplayName("Teacher with ownership can view by practice")
    void getByPracticeTeacherOwner() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(true);
        when(practiceGradeRepository.findByPracticeId(1L)).thenReturn(List.of(samplePracticeGrade()));

        List<PracticeGradeResponse> result =
                practiceGradeService.getByPractice(1L, "teacher@test.ru");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Teacher without ownership is denied getByPractice")
    void getByPracticeTeacherNotOwner() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> practiceGradeService.getByPractice(1L, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Admin can view by practice without ownership")
    void getByPracticeAdmin() {
        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(practiceGradeRepository.findByPracticeId(1L)).thenReturn(List.of(samplePracticeGrade()));

        List<PracticeGradeResponse> result =
                practiceGradeService.getByPractice(1L, "admin@test.ru");
        assertEquals(1, result.size());
    }

    // ── create ───────────────────────────────────────────────────

    @Test
    @DisplayName("Create practice grade succeeds")
    void createSuccess() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(5).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(true);
        when(practiceGradeRepository.findByStudentIdAndPracticeId(3L, 1L)).thenReturn(Optional.empty());
        when(practiceGradeRepository.save(any(PracticeGrade.class))).thenReturn(samplePracticeGrade());

        PracticeGradeResponse response = practiceGradeService.create(request, "teacher@test.ru");

        assertNotNull(response);
        assertEquals(5, response.getGrade());
        assertEquals("Лабораторная 1", response.getPracticeTitle());
        assertEquals(10, response.getMaxGrade());
        assertFalse(response.getPracticeIsCredit());
        verify(auditService).log(eq(2L), eq("CREATE_PRACTICE_GRADE"), eq("PracticeGrade"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Admin can create without ownership check")
    void createByAdmin() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(4).build();

        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(practiceGradeRepository.findByStudentIdAndPracticeId(3L, 1L)).thenReturn(Optional.empty());
        when(practiceGradeRepository.save(any(PracticeGrade.class))).thenReturn(samplePracticeGrade());

        PracticeGradeResponse response = practiceGradeService.create(request, "admin@test.ru");
        assertNotNull(response);
        verify(teacherSubjectRepository, never()).existsByTeacherIdAndSubjectInDirection_Id(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Grade exceeds max_grade throws BadRequestException")
    void gradeExceedsMaxGrade() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(15).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> practiceGradeService.create(request, "teacher@test.ru"));
        verify(practiceGradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative grade throws BadRequestException")
    void gradeNegative() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(-1).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> practiceGradeService.create(request, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Credit practice with numeric grade throws BadRequestException")
    void creditPracticeRejectsNumericGrade() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(2L).grade(5).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectPracticeRepository.findById(2L)).thenReturn(Optional.of(creditPractice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> practiceGradeService.create(request, "teacher@test.ru"));
        verify(practiceGradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate student+practice throws ConflictException")
    void createDuplicate() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(4).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(true);
        when(practiceGradeRepository.findByStudentIdAndPracticeId(3L, 1L))
                .thenReturn(Optional.of(PracticeGrade.builder().id(99L).build()));

        assertThrows(ConflictException.class, () -> practiceGradeService.create(request, "teacher@test.ru"));
        verify(practiceGradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Non-owner teacher cannot create")
    void createByNonOwnerTeacher() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(5).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectPracticeRepository.findById(1L)).thenReturn(Optional.of(practice));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> practiceGradeService.create(request, "teacher@test.ru"));
        verify(practiceGradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create for non-student target throws BadRequestException")
    void createForNonStudent() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(2L).practiceId(1L).grade(5).build();

        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(usersRepository.findById(2L)).thenReturn(Optional.of(teacherUser));

        assertThrows(BadRequestException.class, () -> practiceGradeService.create(request, "admin@test.ru"));
    }

    // ── update ───────────────────────────────────────────────────

    @Test
    @DisplayName("Update practice grade succeeds")
    void updateSuccess() {
        PracticeGrade existing = PracticeGrade.builder()
                .id(1L).student(studentUser).practice(practice).grade(5).build();
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(4).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(practiceGradeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(true);
        when(practiceGradeRepository.save(any(PracticeGrade.class))).thenReturn(existing);

        PracticeGradeResponse response = practiceGradeService.update(1L, request, "teacher@test.ru");
        assertNotNull(response);
        assertEquals(4, existing.getGrade());
    }

    @Test
    @DisplayName("Update not found throws ResourceNotFoundException")
    void updateNotFound() {
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(5).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(practiceGradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> practiceGradeService.update(99L, request, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Non-owner teacher cannot update")
    void updateByNonOwnerTeacher() {
        PracticeGrade existing = PracticeGrade.builder()
                .id(1L).student(studentUser).practice(practice).grade(5).build();
        PracticeGradeRequest request = PracticeGradeRequest.builder()
                .studentId(3L).practiceId(1L).grade(9).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(practiceGradeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> practiceGradeService.update(1L, request, "teacher@test.ru"));
    }
}
