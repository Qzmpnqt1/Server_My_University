package org.example.service;

import org.example.dto.request.GradeRequest;
import org.example.dto.response.GradeResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.UniversityScopeService;
import org.example.service.impl.GradeServiceImpl;
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
class GradeServiceTest {

    @Mock private GradeRepository gradeRepository;
    @Mock private UsersRepository usersRepository;
    @Mock private SubjectInDirectionRepository subjectInDirectionRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private UniversityScopeService universityScopeService;

    @InjectMocks
    private GradeServiceImpl gradeService;

    private Users teacherUser;
    private Users studentUser;
    private Users adminUser;
    private TeacherProfile teacherProfile;
    private Subject subject;
    private SubjectInDirection subjectInDirection;

    @BeforeEach
    void setUp() {
        subject = Subject.builder().id(1L).name("Математика").build();
        subjectInDirection = SubjectInDirection.builder()
                .id(1L).subject(subject).semester(1).course(1).build();

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

        lenient().doNothing().when(notificationService).notifyGradeChanged(anyLong(), anyString(), anyBoolean());
        lenient().when(universityScopeService.requireAdminUniversityId("admin@test.ru")).thenReturn(1L);
        lenient().doNothing().when(universityScopeService).assertUserInUniversity(anyLong(), eq(1L));
        lenient().doNothing().when(universityScopeService).assertSubjectDirectionInUniversity(anyLong(), eq(1L));
    }

    private Grade sampleGrade() {
        return Grade.builder()
                .id(1L).student(studentUser).subjectDirection(subjectInDirection).grade(5).build();
    }

    // ── getMyGrades ──────────────────────────────────────────────

    @Test
    @DisplayName("Student can view own grades")
    void getMyGradesStudent() {
        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(studentUser));
        when(gradeRepository.findByStudentId(3L)).thenReturn(List.of(sampleGrade()));

        List<GradeResponse> result = gradeService.getMyGrades("student@test.ru");

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getGrade());
        assertEquals("Иванов Иван", result.get(0).getStudentName());
    }

    @Test
    @DisplayName("Non-student cannot use getMyGrades")
    void getMyGradesNonStudent() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        assertThrows(AccessDeniedException.class, () -> gradeService.getMyGrades("teacher@test.ru"));
    }

    // ── getByStudent ─────────────────────────────────────────────

    @Test
    @DisplayName("Admin can view any student's grades")
    void getByStudentAdmin() {
        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(gradeRepository.findByStudentId(3L)).thenReturn(List.of(sampleGrade()));

        List<GradeResponse> result = gradeService.getByStudent(3L, "admin@test.ru");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Teacher can view student's grades")
    void getByStudentTeacher() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(gradeRepository.findByStudentId(3L)).thenReturn(List.of(sampleGrade()));

        List<GradeResponse> result = gradeService.getByStudent(3L, "teacher@test.ru");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Student cannot view other student's grades")
    void getByStudentOtherStudentDenied() {
        Users otherStudent = Users.builder()
                .id(5L).email("other@test.ru").userType(UserType.STUDENT).build();

        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(studentUser));
        when(usersRepository.findById(5L)).thenReturn(Optional.of(otherStudent));

        assertThrows(AccessDeniedException.class, () -> gradeService.getByStudent(5L, "student@test.ru"));
    }

    @Test
    @DisplayName("getByStudent with non-student target throws BadRequestException")
    void getByStudentNonStudentTarget() {
        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(usersRepository.findById(2L)).thenReturn(Optional.of(teacherUser));

        assertThrows(BadRequestException.class, () -> gradeService.getByStudent(2L, "admin@test.ru"));
    }

    // ── getBySubjectDirection ────────────────────────────────────

    @Test
    @DisplayName("Teacher with ownership can view grades by subject direction")
    void getBySubjectDirectionTeacherOwner() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(true);
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(sampleGrade()));

        List<GradeResponse> result = gradeService.getBySubjectDirection(1L, "teacher@test.ru");
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Teacher without ownership is denied getBySubjectDirection")
    void getBySubjectDirectionTeacherNotOwner() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> gradeService.getBySubjectDirection(1L, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Admin can view grades by subject direction without ownership check")
    void getBySubjectDirectionAdmin() {
        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(gradeRepository.findBySubjectDirectionId(1L)).thenReturn(List.of(sampleGrade()));

        List<GradeResponse> result = gradeService.getBySubjectDirection(1L, "admin@test.ru");
        assertEquals(1, result.size());
    }

    // ── create ───────────────────────────────────────────────────

    @Test
    @DisplayName("Create grade by assigned teacher succeeds")
    void createByTeacher() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(5).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(true);
        when(gradeRepository.findByStudentIdAndSubjectDirectionId(3L, 1L)).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenReturn(sampleGrade());

        GradeResponse response = gradeService.create(request, "teacher@test.ru");

        assertNotNull(response);
        assertEquals(5, response.getGrade());
        assertEquals("Математика", response.getSubjectName());
        verify(auditService).log(eq(2L), eq("CREATE_GRADE"), eq("Grade"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Admin can create grade without ownership check")
    void createByAdmin() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(4).build();

        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(gradeRepository.findByStudentIdAndSubjectDirectionId(3L, 1L)).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenReturn(sampleGrade());

        GradeResponse response = gradeService.create(request, "admin@test.ru");
        assertNotNull(response);
        verify(teacherSubjectRepository, never()).existsByTeacherIdAndSubjectId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Non-owner teacher cannot create grade")
    void createByNonOwnerTeacher() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(4).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> gradeService.create(request, "teacher@test.ru"));
        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate grade throws ConflictException")
    void createDuplicate() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(3).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(true);
        when(gradeRepository.findByStudentIdAndSubjectDirectionId(3L, 1L))
                .thenReturn(Optional.of(Grade.builder().id(99L).build()));

        assertThrows(ConflictException.class, () -> gradeService.create(request, "teacher@test.ru"));
        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Grade > 5 throws BadRequestException")
    void createGradeOverMax() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(6).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> gradeService.create(request, "teacher@test.ru"));
        verify(gradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Grade < 1 throws BadRequestException")
    void createGradeUnderMin() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(0).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> gradeService.create(request, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Neither grade nor creditStatus throws BadRequestException")
    void createNeitherGradeNorCredit() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(usersRepository.findById(3L)).thenReturn(Optional.of(studentUser));
        when(subjectInDirectionRepository.findById(1L)).thenReturn(Optional.of(subjectInDirection));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> gradeService.create(request, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Create grade for non-student target throws BadRequestException")
    void createForNonStudent() {
        GradeRequest request = GradeRequest.builder()
                .studentId(2L).subjectDirectionId(1L).grade(4).build();

        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(usersRepository.findById(2L)).thenReturn(Optional.of(teacherUser));

        assertThrows(BadRequestException.class, () -> gradeService.create(request, "admin@test.ru"));
    }

    // ── update ───────────────────────────────────────────────────

    @Test
    @DisplayName("Update grade by teacher succeeds")
    void updateByTeacher() {
        Grade existing = Grade.builder()
                .id(1L).student(studentUser).subjectDirection(subjectInDirection).grade(3).build();
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(4).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(true);
        when(gradeRepository.save(any(Grade.class))).thenReturn(existing);

        GradeResponse response = gradeService.update(1L, request, "teacher@test.ru");
        assertNotNull(response);
        assertEquals(4, existing.getGrade());
    }

    @Test
    @DisplayName("Update grade not found throws ResourceNotFoundException")
    void updateNotFound() {
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(4).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(gradeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gradeService.update(99L, request, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Non-owner teacher cannot update grade")
    void updateByNonOwnerTeacher() {
        Grade existing = Grade.builder()
                .id(1L).student(studentUser).subjectDirection(subjectInDirection).grade(3).build();
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(4).build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(10L, 1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> gradeService.update(1L, request, "teacher@test.ru"));
    }

    @Test
    @DisplayName("Admin can update grade without ownership check")
    void updateByAdmin() {
        Grade existing = Grade.builder()
                .id(1L).student(studentUser).subjectDirection(subjectInDirection).grade(3).build();
        GradeRequest request = GradeRequest.builder()
                .studentId(3L).subjectDirectionId(1L).grade(5).build();

        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gradeRepository.save(any(Grade.class))).thenReturn(existing);

        GradeResponse response = gradeService.update(1L, request, "admin@test.ru");
        assertNotNull(response);
        verify(teacherSubjectRepository, never()).existsByTeacherIdAndSubjectId(anyLong(), anyLong());
    }
}
