package org.example.service.impl;

import org.example.exception.AccessDeniedException;
import org.example.model.*;
import org.example.repository.ScheduleRepository;
import org.example.repository.StudentProfileRepository;
import org.example.repository.UsersRepository;
import org.example.service.UniversityScopeService;
import org.example.service.ViewerUniversityResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleAuthorizationServiceImplTest {

    @Mock
    private UsersRepository usersRepository;
    @Mock
    private UniversityScopeService universityScopeService;
    @Mock
    private ViewerUniversityResolver viewerUniversityResolver;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private ScheduleRepository scheduleRepository;

    private ScheduleAuthorizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduleAuthorizationServiceImpl(
                usersRepository,
                universityScopeService,
                viewerUniversityResolver,
                studentProfileRepository,
                scheduleRepository
        );
    }

    @Test
    void ensureAdmin_teacher_throws() {
        Users teacher = Users.builder().id(1L).email("t@u.ru").userType(UserType.TEACHER).build();
        when(usersRepository.findByEmail("t@u.ru")).thenReturn(Optional.of(teacher));
        assertThrows(AccessDeniedException.class, () -> service.ensureAdmin("t@u.ru"));
    }

    @Test
    void ensureCanViewGroupSchedule_student_delegatesToScope() {
        Users student = Users.builder().id(10L).email("s@u.ru").userType(UserType.STUDENT).build();
        when(usersRepository.findByEmail("s@u.ru")).thenReturn(Optional.of(student));
        when(viewerUniversityResolver.requireUniversityIdForEmail("s@u.ru")).thenReturn(7L);
        doNothing().when(universityScopeService).assertAcademicGroupInUniversity(5L, 7L);

        assertDoesNotThrow(() -> service.ensureCanViewGroupSchedule("s@u.ru", 5L));
        verify(universityScopeService).assertAcademicGroupInUniversity(5L, 7L);
    }

    @Test
    void ensureCanViewScheduleEntry_studentOtherGroup_throws() {
        University university = University.builder().id(1L).name("U").build();
        Institute institute = Institute.builder().id(2L).name("I").university(university).build();
        StudyDirection direction = StudyDirection.builder().id(1L).name("D").institute(institute).build();
        AcademicGroup g1 = AcademicGroup.builder()
                .id(1L)
                .name("G1")
                .course(1)
                .yearOfAdmission(2024)
                .direction(direction)
                .build();
        AcademicGroup g2 = AcademicGroup.builder()
                .id(2L)
                .name("G2")
                .course(1)
                .yearOfAdmission(2024)
                .direction(direction)
                .build();
        Users student = Users.builder().id(10L).email("s@u.ru").userType(UserType.STUDENT).build();
        Users teacher = Users.builder().id(20L).userType(UserType.TEACHER).build();
        Schedule schedule = Schedule.builder().id(100L).group(g2).teacher(teacher).build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(student).group(g1).build();

        when(usersRepository.findByEmail("s@u.ru")).thenReturn(Optional.of(student));
        when(scheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(studentProfileRepository.findFetchedByUserId(10L)).thenReturn(Optional.of(sp));

        assertThrows(AccessDeniedException.class, () -> service.ensureCanViewScheduleEntry("s@u.ru", 100L));
    }

    @Test
    void ensureCanViewScheduleEntry_studentSameGroup_ok() {
        University university = University.builder().id(1L).name("U").build();
        Institute institute = Institute.builder().id(2L).name("I").university(university).build();
        StudyDirection direction = StudyDirection.builder().id(1L).name("D").institute(institute).build();
        AcademicGroup g1 = AcademicGroup.builder()
                .id(1L)
                .name("G1")
                .course(1)
                .yearOfAdmission(2024)
                .direction(direction)
                .build();
        Users student = Users.builder().id(10L).email("s@u.ru").userType(UserType.STUDENT).build();
        Users teacher = Users.builder().id(20L).userType(UserType.TEACHER).build();
        Schedule schedule = Schedule.builder().id(100L).group(g1).teacher(teacher).build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(student).group(g1).build();

        when(usersRepository.findByEmail("s@u.ru")).thenReturn(Optional.of(student));
        when(scheduleRepository.findById(100L)).thenReturn(Optional.of(schedule));
        when(studentProfileRepository.findFetchedByUserId(10L)).thenReturn(Optional.of(sp));

        assertDoesNotThrow(() -> service.ensureCanViewScheduleEntry("s@u.ru", 100L));
    }

    @Test
    void ensureCanViewTeacherSchedule_admin_teacherNotInCampus_throws() {
        Users admin = Users.builder().id(1L).email("a@u.ru").userType(UserType.ADMIN).build();
        when(usersRepository.findByEmail("a@u.ru")).thenReturn(Optional.of(admin));
        when(universityScopeService.requireCampusUniversityId("a@u.ru")).thenReturn(9L);
        when(universityScopeService.teacherUserInUniversity(50L, 9L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.ensureCanViewTeacherSchedule("a@u.ru", 50L));
    }
}
