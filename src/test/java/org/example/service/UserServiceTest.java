package org.example.service;

import org.example.dto.response.UserProfileResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private UniversityScopeService universityScopeService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void universityScopeLenient() {
        lenient().when(universityScopeService.requireAdminUniversityId(anyString())).thenReturn(1L);
        lenient().doNothing().when(universityScopeService).assertUserInUniversity(anyLong(), anyLong());
    }

    private static Users buildUser(
            Long id,
            String email,
            UserType userType,
            Boolean isActive,
            String firstName,
            String lastName,
            String middleName) {
        return Users.builder()
                .id(id)
                .email(email)
                .passwordHash("hash")
                .firstName(firstName)
                .lastName(lastName)
                .middleName(middleName)
                .userType(userType)
                .isActive(isActive)
                .createdAt(LocalDateTime.of(2024, 1, 15, 10, 0))
                .build();
    }

    @Test
    void getAllUsers_ReturnsAllUsers() {
        Users u1 = buildUser(1L, "a@uni.ru", UserType.STUDENT, true, "Иван", "Иванов", "Иванович");
        Users u2 = buildUser(2L, "b@uni.ru", UserType.TEACHER, false, "Пётр", "Петров", null);
        Users admin = buildUser(99L, "admin@uni.ru", UserType.ADMIN, true, "Админ", "Админов", null);
        University uni = University.builder().id(1L).name("U").shortName("U").city("C").build();
        Institute inst = Institute.builder().id(1L).name("I").shortName("I").university(uni).build();
        StudyDirection dir = StudyDirection.builder().id(1L).name("D").institute(inst).build();
        AcademicGroup group = AcademicGroup.builder().id(1L).name("G").course(1).direction(dir).build();
        StudentProfile sp1 = StudentProfile.builder().id(1L).user(u1).group(group).institute(inst).build();
        TeacherProfile tp2 = TeacherProfile.builder().id(2L).user(u2).institute(inst).build();

        when(usersRepository.findByEmail("admin@uni.ru")).thenReturn(Optional.of(admin));
        when(usersRepository.findAll()).thenReturn(List.of(u1, u2));
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(sp1));
        when(teacherProfileRepository.findByUserId(2L)).thenReturn(Optional.of(tp2));

        List<UserProfileResponse> result = userService.getAllUsers(null, null, null, null, null, null, "admin@uni.ru");

        assertEquals(2, result.size());
        UserProfileResponse r0 = result.get(0);
        assertEquals(1L, r0.getId());
        assertEquals("a@uni.ru", r0.getEmail());
        assertEquals("Иван", r0.getFirstName());
        assertEquals("Иванов", r0.getLastName());
        assertEquals("Иванович", r0.getMiddleName());
        assertEquals(UserType.STUDENT, r0.getUserType());
        assertTrue(r0.getIsActive());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 0), r0.getCreatedAt());
        assertNull(r0.getStudentProfile());

        UserProfileResponse r1 = result.get(1);
        assertEquals(2L, r1.getId());
        assertEquals("b@uni.ru", r1.getEmail());
        assertEquals("Пётр", r1.getFirstName());
        assertEquals("Петров", r1.getLastName());
        assertNull(r1.getMiddleName());
        assertEquals(UserType.TEACHER, r1.getUserType());
        assertFalse(r1.getIsActive());
        assertNotNull(r1.getTeacherProfile());
        assertEquals(2L, r1.getTeacherProfile().getTeacherProfileId());
    }

    @Test
    void getUserById_Success() {
        Users student = buildUser(1L, "stu@uni.ru", UserType.STUDENT, true, "Анна", "Смирнова", null);
        AcademicGroup group = AcademicGroup.builder()
                .id(1L)
                .name("ИТ-101")
                .course(1)
                .yearOfAdmission(2024)
                .build();
        Institute institute = Institute.builder()
                .id(1L)
                .name("Институт информатики")
                .build();
        StudentProfile profile = StudentProfile.builder()
                .id(10L)
                .user(student)
                .group(group)
                .institute(institute)
                .build();

        when(usersRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findFetchedByUserId(1L)).thenReturn(Optional.of(profile));

        UserProfileResponse response = userService.getUserById(1L, "admin@uni.ru");

        assertNotNull(response.getStudentProfile());
        assertEquals(1L, response.getStudentProfile().getGroupId());
        assertEquals("ИТ-101", response.getStudentProfile().getGroupName());
        assertEquals(1L, response.getStudentProfile().getInstituteId());
        assertEquals("Институт информатики", response.getStudentProfile().getInstituteName());
        assertNull(response.getTeacherProfile());
        assertNull(response.getAdminProfile());
        verify(teacherProfileRepository, never()).findFetchedByUserId(anyLong());
        verify(adminProfileRepository, never()).findFetchedByUserId(anyLong());
    }

    @Test
    void getUserById_NotFound() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(1L, "admin@uni.ru"));
        assertTrue(ex.getMessage().contains("1"));
    }

    @Test
    void activateUser_Success() {
        Users inactive = buildUser(5L, "inactive@uni.ru", UserType.STUDENT, false, "A", "B", null);
        Users admin = buildUser(99L, "admin@uni.ru", UserType.ADMIN, true, "Admin", "User", null);

        when(usersRepository.findById(5L)).thenReturn(Optional.of(inactive));
        when(usersRepository.save(any(Users.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usersRepository.findByEmail("admin@uni.ru")).thenReturn(Optional.of(admin));

        userService.activateUser(5L, "admin@uni.ru");

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsActive());
        assertEquals(5L, captor.getValue().getId());

        verify(auditService).log(
                eq(99L),
                eq("ACTIVATE_USER"),
                eq("Users"),
                eq(5L),
                contains("Активирован"));
    }

    @Test
    void activateUser_AlreadyActive() {
        Users active = buildUser(5L, "active@uni.ru", UserType.STUDENT, true, "A", "B", null);
        when(usersRepository.findById(5L)).thenReturn(Optional.of(active));

        assertThrows(BadRequestException.class, () -> userService.activateUser(5L, "admin@uni.ru"));

        verify(usersRepository, never()).save(any());
        verify(auditService, never()).log(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void deactivateUser_Success() {
        Users target = buildUser(2L, "target@uni.ru", UserType.STUDENT, true, "T", "T", null);
        Users admin = buildUser(1L, "admin@uni.ru", UserType.ADMIN, true, "A", "A", null);

        when(usersRepository.findById(2L)).thenReturn(Optional.of(target));
        when(usersRepository.save(any(Users.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usersRepository.findByEmail("admin@uni.ru")).thenReturn(Optional.of(admin));

        userService.deactivateUser(2L, "admin@uni.ru");

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(usersRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsActive());
        assertEquals(2L, captor.getValue().getId());

        verify(auditService).log(
                eq(1L),
                eq("DEACTIVATE_USER"),
                eq("Users"),
                eq(2L),
                contains("Деактивирован"));
    }

    @Test
    void deactivateUser_AlreadyInactive() {
        Users inactive = buildUser(2L, "in@uni.ru", UserType.STUDENT, false, "X", "Y", null);
        when(usersRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThrows(BadRequestException.class, () -> userService.deactivateUser(2L, "admin@uni.ru"));

        verify(usersRepository, never()).save(any());
        verify(auditService, never()).log(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void deactivateUser_SelfDeactivation() {
        Users adminSelf = buildUser(1L, "admin@uni.ru", UserType.ADMIN, true, "A", "A", null);
        when(usersRepository.findById(1L)).thenReturn(Optional.of(adminSelf));
        when(usersRepository.findByEmail("admin@uni.ru")).thenReturn(Optional.of(adminSelf));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> userService.deactivateUser(1L, "admin@uni.ru"));

        assertEquals("Нельзя деактивировать собственный аккаунт", ex.getMessage());
        verify(usersRepository, never()).save(any());
        verify(auditService, never()).log(any(), anyString(), anyString(), any(), anyString());
    }
}
