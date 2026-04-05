package org.example.service;

import org.example.dto.request.ChangeEmailRequest;
import org.example.dto.request.ChangePasswordRequest;
import org.example.dto.response.UserProfileResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private LocalDateTime fixedCreatedAt;

    @BeforeEach
    void setUp() {
        fixedCreatedAt = LocalDateTime.of(2025, 3, 1, 12, 0);
    }

    @Test
    @DisplayName("getMyProfile: student with group and institute maps to response")
    void getMyProfile_Student_Success() {
        University university = University.builder()
                .id(1L)
                .name("Тестовый университет")
                .shortName("ТУ")
                .city("Москва")
                .build();
        Institute institute = Institute.builder()
                .id(1L)
                .name("Институт информатики")
                .university(university)
                .build();
        StudyDirection direction = StudyDirection.builder()
                .id(1L)
                .name("Прикладная информатика")
                .institute(institute)
                .build();
        AcademicGroup group = AcademicGroup.builder()
                .id(1L)
                .name("ИТ-101")
                .course(2)
                .yearOfAdmission(2023)
                .direction(direction)
                .build();

        Users studentUser = Users.builder()
                .id(10L)
                .email("student@test.ru")
                .passwordHash("hash")
                .firstName("Иван")
                .lastName("Студентов")
                .middleName("Иванович")
                .userType(UserType.STUDENT)
                .isActive(true)
                .createdAt(fixedCreatedAt)
                .build();

        StudentProfile studentProfile = StudentProfile.builder()
                .id(1L)
                .user(studentUser)
                .group(group)
                .institute(institute)
                .build();

        when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findFetchedByUserId(10L)).thenReturn(Optional.of(studentProfile));

        UserProfileResponse response = userProfileService.getMyProfile("student@test.ru");

        assertEquals(10L, response.getId());
        assertEquals("student@test.ru", response.getEmail());
        assertEquals("Иван", response.getFirstName());
        assertEquals("Студентов", response.getLastName());
        assertEquals("Иванович", response.getMiddleName());
        assertEquals(UserType.STUDENT, response.getUserType());
        assertTrue(response.getIsActive());
        assertEquals(fixedCreatedAt, response.getCreatedAt());

        assertNotNull(response.getStudentProfile());
        assertEquals(1L, response.getStudentProfile().getGroupId());
        assertEquals("ИТ-101", response.getStudentProfile().getGroupName());
        assertEquals(2, response.getStudentProfile().getCourse());
        assertEquals(1L, response.getStudentProfile().getInstituteId());
        assertEquals("Институт информатики", response.getStudentProfile().getInstituteName());

        assertNull(response.getTeacherProfile());
        assertNull(response.getAdminProfile());

        verify(studentProfileRepository).findFetchedByUserId(10L);
        verifyNoInteractions(teacherProfileRepository, adminProfileRepository);
    }

    @Test
    @DisplayName("getMyProfile: teacher with institute and position maps to response")
    void getMyProfile_Teacher_Success() {
        University university = University.builder()
                .id(1L)
                .name("Педагогический университет")
                .shortName("ПУ")
                .city("Казань")
                .build();
        Institute institute = Institute.builder()
                .id(2L)
                .name("Институт физики")
                .university(university)
                .build();

        Users teacherUser = Users.builder()
                .id(20L)
                .email("teacher@test.ru")
                .passwordHash("hash")
                .firstName("Пётр")
                .lastName("Преподаватель")
                .middleName("Петрович")
                .userType(UserType.TEACHER)
                .isActive(true)
                .createdAt(fixedCreatedAt)
                .build();

        TeacherProfile teacherProfile = TeacherProfile.builder()
                .id(1L)
                .user(teacherUser)
                .institute(institute)
                .position("Доцент")
                .build();

        when(usersRepository.findByEmail("teacher@test.ru")).thenReturn(Optional.of(teacherUser));
        when(teacherProfileRepository.findFetchedByUserId(20L)).thenReturn(Optional.of(teacherProfile));

        UserProfileResponse response = userProfileService.getMyProfile("teacher@test.ru");

        assertEquals(20L, response.getId());
        assertEquals("teacher@test.ru", response.getEmail());
        assertEquals(UserType.TEACHER, response.getUserType());
        assertTrue(response.getIsActive());

        assertNotNull(response.getTeacherProfile());
        assertEquals(2L, response.getTeacherProfile().getInstituteId());
        assertEquals("Институт физики", response.getTeacherProfile().getInstituteName());
        assertEquals("Доцент", response.getTeacherProfile().getPosition());

        assertNull(response.getStudentProfile());
        assertNull(response.getAdminProfile());

        verify(teacherProfileRepository).findFetchedByUserId(20L);
        verifyNoInteractions(studentProfileRepository, adminProfileRepository);
    }

    @Test
    @DisplayName("getMyProfile: admin with university and role maps to response")
    void getMyProfile_Admin_Success() {
        University university = University.builder()
                .id(1L)
                .name("Федеральный университет")
                .shortName("ФУ")
                .city("Екатеринбург")
                .build();

        Users adminUser = Users.builder()
                .id(30L)
                .email("admin@test.ru")
                .passwordHash("hash")
                .firstName("Алексей")
                .lastName("Админов")
                .middleName(null)
                .userType(UserType.ADMIN)
                .isActive(true)
                .createdAt(fixedCreatedAt)
                .build();

        AdminProfile adminProfile = AdminProfile.builder()
                .id(1L)
                .user(adminUser)
                .university(university)
                .role("ADMIN")
                .build();

        when(usersRepository.findByEmail("admin@test.ru")).thenReturn(Optional.of(adminUser));
        when(adminProfileRepository.findFetchedByUserId(30L)).thenReturn(Optional.of(adminProfile));

        UserProfileResponse response = userProfileService.getMyProfile("admin@test.ru");

        assertEquals(30L, response.getId());
        assertEquals("admin@test.ru", response.getEmail());
        assertEquals(UserType.ADMIN, response.getUserType());

        assertNotNull(response.getAdminProfile());
        assertEquals(1L, response.getAdminProfile().getUniversityId());
        assertEquals("Федеральный университет", response.getAdminProfile().getUniversityName());
        assertEquals("ADMIN", response.getAdminProfile().getRole());

        assertNull(response.getStudentProfile());
        assertNull(response.getTeacherProfile());

        verify(adminProfileRepository).findFetchedByUserId(30L);
        verifyNoInteractions(studentProfileRepository, teacherProfileRepository);
    }

    @Test
    @DisplayName("getMyProfile: missing user throws ResourceNotFoundException")
    void getMyProfile_UserNotFound() {
        when(usersRepository.findByEmail("missing@test.ru")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getMyProfile("missing@test.ru"));

        verifyNoInteractions(studentProfileRepository, teacherProfileRepository, adminProfileRepository);
    }

    @Test
    @DisplayName("changeEmail: updates and saves when new email is free")
    void changeEmail_Success() {
        Users user = Users.builder()
                .id(1L)
                .email("old@test.ru")
                .passwordHash("$2a$10$hash")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .isActive(true)
                .createdAt(fixedCreatedAt)
                .build();

        when(usersRepository.findByEmail("old@test.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "$2a$10$hash")).thenReturn(true);
        when(usersRepository.existsByEmail("new@test.ru")).thenReturn(false);

        ChangeEmailRequest request = ChangeEmailRequest.builder()
                .newEmail("new@test.ru")
                .currentPassword("correctPassword")
                .build();
        userProfileService.changeEmail("old@test.ru", request);

        assertEquals("new@test.ru", user.getEmail());
        verify(usersRepository).existsByEmail("new@test.ru");
        verify(usersRepository).save(user);
    }

    @Test
    @DisplayName("changeEmail: ConflictException when email already taken; user not saved")
    void changeEmail_EmailAlreadyTaken() {
        Users user = Users.builder()
                .id(1L)
                .email("old@test.ru")
                .passwordHash("$2a$10$hash")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .isActive(true)
                .createdAt(fixedCreatedAt)
                .build();

        when(usersRepository.findByEmail("old@test.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "$2a$10$hash")).thenReturn(true);
        when(usersRepository.existsByEmail("taken@test.ru")).thenReturn(true);

        ChangeEmailRequest request = ChangeEmailRequest.builder()
                .newEmail("taken@test.ru")
                .currentPassword("correctPassword")
                .build();

        assertThrows(ConflictException.class, () -> userProfileService.changeEmail("old@test.ru", request));

        assertEquals("old@test.ru", user.getEmail());
        verify(usersRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword: encodes new password and saves when old password matches")
    void changePassword_Success() {
        Users user = Users.builder()
                .id(1L)
                .email("user@test.ru")
                .passwordHash("$2a$10$existing")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .isActive(true)
                .createdAt(fixedCreatedAt)
                .build();

        when(usersRepository.findByEmail("user@test.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldSecret", "$2a$10$existing")).thenReturn(true);
        when(passwordEncoder.encode("newSecret123")).thenReturn("$2a$10$newEncoded");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldSecret")
                .newPassword("newSecret123")
                .newPasswordConfirm("newSecret123")
                .build();

        userProfileService.changePassword("user@test.ru", request);

        verify(passwordEncoder).matches("oldSecret", "$2a$10$existing");
        verify(passwordEncoder).encode("newSecret123");
        assertEquals("$2a$10$newEncoded", user.getPasswordHash());
        verify(usersRepository).save(user);
    }

    @Test
    @DisplayName("changePassword: BadRequestException when old password wrong; user not saved")
    void changePassword_WrongOldPassword() {
        Users user = Users.builder()
                .id(1L)
                .email("user@test.ru")
                .passwordHash("$2a$10$existing")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .isActive(true)
                .createdAt(fixedCreatedAt)
                .build();

        when(usersRepository.findByEmail("user@test.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOld", "$2a$10$existing")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrongOld")
                .newPassword("newSecret123")
                .newPasswordConfirm("newSecret123")
                .build();

        assertThrows(BadRequestException.class, () -> userProfileService.changePassword("user@test.ru", request));

        verify(passwordEncoder, never()).encode(any());
        verify(usersRepository, never()).save(any());
        assertEquals("$2a$10$existing", user.getPasswordHash());
    }
}
