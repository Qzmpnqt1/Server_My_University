package org.example.service;

import org.example.dto.request.ScheduleRequest;
import org.example.dto.response.ScheduleResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.UniversityScopeService;
import org.example.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private SubjectLessonTypeRepository subjectLessonTypeRepository;
    @Mock private UsersRepository usersRepository;
    @Mock private AcademicGroupRepository academicGroupRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private SubjectInDirectionRepository subjectInDirectionRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private org.example.service.AuditService auditService;
    @Mock private org.example.service.NotificationService notificationService;
    @Mock private UniversityScopeService universityScopeService;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Users teacher;
    private AcademicGroup group;
    private Classroom classroom;
    private SubjectLessonType subjectType;
    private Subject subject;
    private SubjectInDirection subjectInDirection;
    private Schedule sampleSchedule;

    @BeforeEach
    void setUp() {
        teacher = Users.builder()
                .id(1L).email("teacher@test.ru")
                .firstName("Алексей").lastName("Попов").middleName("Сергеевич")
                .userType(UserType.TEACHER).isActive(true).build();

        StudyDirection direction = StudyDirection.builder().id(1L).name("ИТ").build();
        group = AcademicGroup.builder().id(1L).name("ИТ-201").course(2).direction(direction).build();

        classroom = Classroom.builder().id(1L).building("Корпус А").roomNumber("305").build();

        subject = Subject.builder().id(1L).name("Математика").build();

        subjectInDirection = SubjectInDirection.builder()
                .id(1L).subject(subject).direction(direction).semester(1).course(1).build();

        subjectType = SubjectLessonType.builder()
                .id(1L).subjectDirection(subjectInDirection).lessonType(LessonType.LECTURE).build();

        sampleSchedule = Schedule.builder()
                .id(1L).subjectType(subjectType).teacher(teacher).group(group).classroom(classroom)
                .dayOfWeek(1).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30)).weekNumber(1)
                .build();

        lenient().when(universityScopeService.requireAdminUniversityId("admin@test.ru")).thenReturn(1L);
        lenient().doNothing().when(universityScopeService).assertSubjectDirectionInUniversity(anyLong(), eq(1L));
        lenient().doNothing().when(universityScopeService).assertAcademicGroupInUniversity(anyLong(), eq(1L));
        lenient().doNothing().when(universityScopeService).assertClassroomInUniversity(anyLong(), eq(1L));
        lenient().doNothing().when(universityScopeService).assertUserInUniversity(anyLong(), eq(1L));
    }

    private ScheduleRequest buildValidRequest() {
        return ScheduleRequest.builder()
                .subjectTypeId(1L).teacherId(1L).groupId(1L).classroomId(1L)
                .dayOfWeek(1).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30)).weekNumber(1)
                .build();
    }

    private void stubEntities() {
        when(subjectLessonTypeRepository.findById(1L)).thenReturn(Optional.of(subjectType));
        when(usersRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
    }

    private void stubNoConflicts(Long excludeId) {
        when(scheduleRepository.findTeacherConflicts(anyInt(), anyInt(), any(), any(), anyLong(), eq(excludeId)))
                .thenReturn(Collections.emptyList());
        when(scheduleRepository.findGroupConflicts(anyInt(), anyInt(), any(), any(), anyLong(), eq(excludeId)))
                .thenReturn(Collections.emptyList());
        when(scheduleRepository.findClassroomConflicts(anyInt(), anyInt(), any(), any(), anyLong(), eq(excludeId)))
                .thenReturn(Collections.emptyList());
    }

    // ── getAll ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAll returns all schedule entries")
    void getAll() {
        when(scheduleRepository.findAllByUniversityId(1L)).thenReturn(List.of(sampleSchedule));
        List<ScheduleResponse> result = scheduleService.getAllForAdmin("admin@test.ru");
        assertEquals(1, result.size());
        assertEquals("Математика", result.get(0).getSubjectName());
    }

    // ── getById ──────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns schedule entry")
    void getByIdSuccess() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(sampleSchedule));
        ScheduleResponse r = scheduleService.getById(1L);
        assertEquals(1L, r.getId());
    }

    @Test
    @DisplayName("getById not found throws ResourceNotFoundException")
    void getByIdNotFound() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> scheduleService.getById(99L));
    }

    // ── getByGroup ───────────────────────────────────────────────

    @Test
    @DisplayName("getByGroup without filters")
    void getByGroupNoFilters() {
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroupId(1L)).thenReturn(List.of(sampleSchedule));

        List<ScheduleResponse> result = scheduleService.getByGroup(1L, null, null);
        assertEquals(1, result.size());
        verify(scheduleRepository).findByGroupId(1L);
    }

    @Test
    @DisplayName("getByGroup with weekNumber filter")
    void getByGroupWeekFilter() {
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroupIdAndWeekNumber(1L, 1)).thenReturn(List.of(sampleSchedule));

        List<ScheduleResponse> result = scheduleService.getByGroup(1L, 1, null);
        assertEquals(1, result.size());
        verify(scheduleRepository).findByGroupIdAndWeekNumber(1L, 1);
    }

    @Test
    @DisplayName("getByGroup with dayOfWeek filter")
    void getByGroupDayFilter() {
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroupIdAndDayOfWeek(1L, 1)).thenReturn(List.of(sampleSchedule));

        List<ScheduleResponse> result = scheduleService.getByGroup(1L, null, 1);
        assertEquals(1, result.size());
        verify(scheduleRepository).findByGroupIdAndDayOfWeek(1L, 1);
    }

    @Test
    @DisplayName("getByGroup with both filters")
    void getByGroupBothFilters() {
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroupIdAndWeekNumberAndDayOfWeek(1L, 1, 2)).thenReturn(List.of());

        List<ScheduleResponse> result = scheduleService.getByGroup(1L, 1, 2);
        assertTrue(result.isEmpty());
        verify(scheduleRepository).findByGroupIdAndWeekNumberAndDayOfWeek(1L, 1, 2);
    }

    @Test
    @DisplayName("getByGroup with non-existent group throws ResourceNotFoundException")
    void getByGroupNotFound() {
        when(academicGroupRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> scheduleService.getByGroup(99L, null, null));
    }

    // ── getByTeacher ─────────────────────────────────────────────

    @Test
    @DisplayName("getByTeacher without filters")
    void getByTeacherNoFilters() {
        when(usersRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(scheduleRepository.findByTeacherId(1L)).thenReturn(List.of(sampleSchedule));

        List<ScheduleResponse> result = scheduleService.getByTeacher(1L, null, null);
        assertEquals(1, result.size());
        assertEquals("Попов Алексей Сергеевич", result.get(0).getTeacherName());
    }

    @Test
    @DisplayName("getByTeacher with weekNumber filter")
    void getByTeacherWeekFilter() {
        when(usersRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(scheduleRepository.findByTeacherIdAndWeekNumber(1L, 2)).thenReturn(List.of());

        List<ScheduleResponse> result = scheduleService.getByTeacher(1L, 2, null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getByTeacher not found throws ResourceNotFoundException")
    void getByTeacherNotFound() {
        when(usersRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> scheduleService.getByTeacher(99L, null, null));
    }

    // ── getMySchedule ────────────────────────────────────────────

    @Test
    @DisplayName("getMySchedule for student returns group schedule")
    void getMyScheduleStudent() {
        Users student = Users.builder()
                .id(2L).email("student@test.ru").userType(UserType.STUDENT)
                .firstName("Иван").lastName("Иванов").isActive(true).build();
        StudentProfile profile = StudentProfile.builder().id(1L).user(student).group(group).build();

        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));
        when(studentProfileRepository.findFetchedByUserId(2L)).thenReturn(Optional.of(profile));
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findByGroupId(1L)).thenReturn(List.of(sampleSchedule));

        List<ScheduleResponse> result = scheduleService.getMySchedule("student@test.ru", null, null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getMySchedule for teacher returns teacher schedule")
    void getMyScheduleTeacher() {
        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacher));
        when(usersRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(scheduleRepository.findByTeacherId(1L)).thenReturn(List.of(sampleSchedule));

        List<ScheduleResponse> result = scheduleService.getMySchedule("teacher@test.ru", null, null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getMySchedule for admin throws AccessDeniedException")
    void getMyScheduleAdmin() {
        Users admin = Users.builder()
                .id(3L).email("admin@test.ru").userType(UserType.ADMIN)
                .firstName("Админ").lastName("Админов").isActive(true).build();

        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class, () -> scheduleService.getMySchedule("admin@test.ru", null, null));
    }

    // ── create ───────────────────────────────────────────────────

    @Test
    @DisplayName("Create schedule successfully")
    void createSuccess() {
        ScheduleRequest request = buildValidRequest();
        stubEntities();
        stubNoConflicts(null);
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(sampleSchedule);
        when(usersRepository.findByEmail("admin@test.ru"))
                .thenReturn(Optional.of(Users.builder().id(99L).email("admin@test.ru").userType(UserType.ADMIN).build()));

        ScheduleResponse response = scheduleService.create(request, "admin@test.ru");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Математика", response.getSubjectName());
        assertEquals("Попов Алексей Сергеевич", response.getTeacherName());
        assertEquals(LessonType.LECTURE, response.getLessonType());
        assertEquals("Корпус А, ауд. 305", response.getClassroomInfo());
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    @DisplayName("Create with non-teacher user throws BadRequestException")
    void createNonTeacherUser() {
        ScheduleRequest request = buildValidRequest();
        Users student = Users.builder().id(1L).userType(UserType.STUDENT).build();

        when(subjectLessonTypeRepository.findById(1L)).thenReturn(Optional.of(subjectType));
        when(usersRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(BadRequestException.class, () -> scheduleService.create(request, "admin@test.ru"));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create with teacher conflict throws ConflictException")
    void createTeacherConflict() {
        ScheduleRequest request = buildValidRequest();
        stubEntities();
        when(usersRepository.findByEmail("admin@test.ru"))
                .thenReturn(Optional.of(Users.builder().id(99L).email("admin@test.ru").userType(UserType.ADMIN).build()));

        when(scheduleRepository.findTeacherConflicts(eq(1), eq(1), any(), any(), eq(1L), isNull()))
                .thenReturn(List.of(Schedule.builder().id(99L).build()));

        assertThrows(ConflictException.class, () -> scheduleService.create(request, "admin@test.ru"));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create with group conflict throws ConflictException")
    void createGroupConflict() {
        ScheduleRequest request = buildValidRequest();
        stubEntities();
        when(usersRepository.findByEmail("admin@test.ru"))
                .thenReturn(Optional.of(Users.builder().id(99L).email("admin@test.ru").userType(UserType.ADMIN).build()));

        when(scheduleRepository.findTeacherConflicts(anyInt(), anyInt(), any(), any(), anyLong(), isNull()))
                .thenReturn(Collections.emptyList());
        when(scheduleRepository.findGroupConflicts(eq(1), eq(1), any(), any(), eq(1L), isNull()))
                .thenReturn(List.of(Schedule.builder().id(99L).build()));

        assertThrows(ConflictException.class, () -> scheduleService.create(request, "admin@test.ru"));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create with classroom conflict throws ConflictException")
    void createClassroomConflict() {
        ScheduleRequest request = buildValidRequest();
        stubEntities();
        when(usersRepository.findByEmail("admin@test.ru"))
                .thenReturn(Optional.of(Users.builder().id(99L).email("admin@test.ru").userType(UserType.ADMIN).build()));

        when(scheduleRepository.findTeacherConflicts(anyInt(), anyInt(), any(), any(), anyLong(), isNull()))
                .thenReturn(Collections.emptyList());
        when(scheduleRepository.findGroupConflicts(anyInt(), anyInt(), any(), any(), anyLong(), isNull()))
                .thenReturn(Collections.emptyList());
        when(scheduleRepository.findClassroomConflicts(eq(1), eq(1), any(), any(), eq(1L), isNull()))
                .thenReturn(List.of(Schedule.builder().id(99L).build()));

        assertThrows(ConflictException.class, () -> scheduleService.create(request, "admin@test.ru"));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid dayOfWeek throws BadRequestException")
    void createInvalidDay() {
        ScheduleRequest request = buildValidRequest();
        request.setDayOfWeek(8);
        assertThrows(BadRequestException.class, () -> scheduleService.create(request, "admin@test.ru"));
    }

    @Test
    @DisplayName("Invalid weekNumber throws BadRequestException")
    void createInvalidWeek() {
        ScheduleRequest request = buildValidRequest();
        request.setWeekNumber(3);
        assertThrows(BadRequestException.class, () -> scheduleService.create(request, "admin@test.ru"));
    }

    @Test
    @DisplayName("Start time after end time throws BadRequestException")
    void createStartAfterEnd() {
        ScheduleRequest request = buildValidRequest();
        request.setStartTime(LocalTime.of(12, 0));
        request.setEndTime(LocalTime.of(10, 0));
        assertThrows(BadRequestException.class, () -> scheduleService.create(request, "admin@test.ru"));
    }

    // ── update ───────────────────────────────────────────────────

    @Test
    @DisplayName("Update excludes own record from conflict check")
    void updateExcludesOwnRecord() {
        ScheduleRequest request = buildValidRequest();

        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(
                Schedule.builder().id(5L).subjectType(subjectType).teacher(teacher).group(group)
                        .classroom(classroom).dayOfWeek(1).startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(10, 30)).weekNumber(1).build()));

        stubEntities();
        stubNoConflicts(5L);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usersRepository.findByEmail("admin@test.ru"))
                .thenReturn(Optional.of(Users.builder().id(99L).email("admin@test.ru").userType(UserType.ADMIN).build()));

        ScheduleResponse response = scheduleService.update(5L, request, "admin@test.ru");

        assertNotNull(response);
        verify(scheduleRepository).findTeacherConflicts(eq(1), eq(1), any(), any(), eq(1L), eq(5L));
        verify(scheduleRepository).findGroupConflicts(eq(1), eq(1), any(), any(), eq(1L), eq(5L));
        verify(scheduleRepository).findClassroomConflicts(eq(1), eq(1), any(), any(), eq(1L), eq(5L));
    }

    @Test
    @DisplayName("Update not found throws ResourceNotFoundException")
    void updateNotFound() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> scheduleService.update(99L, buildValidRequest(), "admin@test.ru"));
    }

    // ── delete ───────────────────────────────────────────────────

    @Test
    @DisplayName("Delete schedule successfully")
    void deleteSuccess() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(sampleSchedule));
        when(usersRepository.findByEmail("admin@test.ru"))
                .thenReturn(Optional.of(Users.builder().id(99L).email("admin@test.ru").userType(UserType.ADMIN).build()));
        assertDoesNotThrow(() -> scheduleService.delete(1L, "admin@test.ru"));
        verify(scheduleRepository).delete(sampleSchedule);
    }

    @Test
    @DisplayName("Delete not found throws ResourceNotFoundException")
    void deleteNotFound() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> scheduleService.delete(99L, "admin@test.ru"));
    }
}
