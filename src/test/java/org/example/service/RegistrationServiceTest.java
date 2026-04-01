package org.example.service;

import org.example.dto.request.ApproveRejectRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.RegistrationRequestResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.*;
import org.example.service.NotificationService;
import org.example.service.UniversityScopeService;
import org.example.service.impl.RegistrationServiceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRequestRepository registrationRequestRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private AcademicGroupRepository academicGroupRepository;

    @Mock
    private InstituteRepository instituteRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private TeacherProfileRepository teacherProfileRepository;

    @Mock
    private AdminProfileRepository adminProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UniversityScopeService universityScopeService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private University testUniversity;
    private AcademicGroup testGroup;
    private StudyDirection testDirection;
    private Institute testInstitute;

    @BeforeEach
    void setUp() {
        testUniversity = University.builder()
                .id(1L)
                .name("МГУ")
                .shortName("МГУ")
                .city("Москва")
                .build();

        testInstitute = Institute.builder()
                .id(1L)
                .name("Институт информатики")
                .university(testUniversity)
                .build();

        testDirection = StudyDirection.builder()
                .id(1L)
                .name("Информатика")
                .institute(testInstitute)
                .build();

        testGroup = AcademicGroup.builder()
                .id(1L)
                .name("ИТ-101")
                .course(1)
                .yearOfAdmission(2025)
                .direction(testDirection)
                .build();

        Users scopeAdmin = Users.builder()
                .id(99L)
                .email("admin@uni.ru")
                .userType(UserType.ADMIN)
                .build();
        lenient().when(usersRepository.findByEmail("admin@uni.ru")).thenReturn(Optional.of(scopeAdmin));
        lenient().when(universityScopeService.requireAdminUniversityId("admin@uni.ru")).thenReturn(1L);
        lenient().doNothing().when(universityScopeService).assertRegistrationRequestInUniversity(anyLong(), eq(1L));
    }

    // ── submitRegistration ────────────────────────────────────────

    @Test
    @DisplayName("Submit registration for student succeeds")
    void submitRegistrationSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.ru")
                .password("password123")
                .firstName("Петр")
                .lastName("Петров")
                .userType(UserType.STUDENT)
                .universityId(1L)
                .groupId(1L)
                .build();

        when(usersRepository.existsByEmail("new@test.ru")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailAndStatus("new@test.ru", RegistrationStatus.PENDING))
                .thenReturn(false);
        when(universityRepository.findById(1L)).thenReturn(Optional.of(testUniversity));
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");

        assertDoesNotThrow(() -> registrationService.submitRegistration(request));
        verify(registrationRequestRepository).save(any(RegistrationRequest.class));
    }

    @Test
    @DisplayName("Submit registration for teacher succeeds")
    void submitRegistrationTeacher() {
        RegisterRequest request = RegisterRequest.builder()
                .email("teacher@test.ru")
                .password("password123")
                .firstName("Анна")
                .lastName("Сидорова")
                .userType(UserType.TEACHER)
                .universityId(1L)
                .instituteId(1L)
                .build();

        when(usersRepository.existsByEmail("teacher@test.ru")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailAndStatus("teacher@test.ru", RegistrationStatus.PENDING))
                .thenReturn(false);
        when(universityRepository.findById(1L)).thenReturn(Optional.of(testUniversity));
        when(instituteRepository.findById(1L)).thenReturn(Optional.of(testInstitute));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");

        assertDoesNotThrow(() -> registrationService.submitRegistration(request));
        verify(registrationRequestRepository).save(any(RegistrationRequest.class));
    }

    @Test
    @DisplayName("Submit registration with existing email throws ConflictException")
    void submitRegistrationDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.ru")
                .password("password123")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .universityId(1L)
                .groupId(1L)
                .build();

        when(usersRepository.existsByEmail("existing@test.ru")).thenReturn(true);

        assertThrows(ConflictException.class, () -> registrationService.submitRegistration(request));
        verify(registrationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit registration with pending duplicate throws ConflictException")
    void submitRegistrationDuplicatePending() {
        RegisterRequest request = RegisterRequest.builder()
                .email("pending@test.ru")
                .password("password123")
                .firstName("Анна")
                .lastName("Сидорова")
                .userType(UserType.TEACHER)
                .universityId(1L)
                .build();

        when(usersRepository.existsByEmail("pending@test.ru")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailAndStatus("pending@test.ru", RegistrationStatus.PENDING))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> registrationService.submitRegistration(request));
        verify(registrationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit registration for student without group throws BadRequestException")
    void submitRegistrationStudentWithoutGroup() {
        RegisterRequest request = RegisterRequest.builder()
                .email("student@test.ru")
                .password("password123")
                .firstName("Мария")
                .lastName("Кузнецова")
                .userType(UserType.STUDENT)
                .universityId(1L)
                .groupId(null)
                .build();

        when(usersRepository.existsByEmail("student@test.ru")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailAndStatus("student@test.ru", RegistrationStatus.PENDING))
                .thenReturn(false);
        when(universityRepository.findById(1L)).thenReturn(Optional.of(testUniversity));

        assertThrows(BadRequestException.class, () -> registrationService.submitRegistration(request));
        verify(registrationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit registration for admin throws BadRequestException")
    void submitRegistrationAdminForbidden() {
        RegisterRequest request = RegisterRequest.builder()
                .email("admin@test.ru")
                .password("password123")
                .firstName("Админ")
                .lastName("Админов")
                .userType(UserType.ADMIN)
                .universityId(1L)
                .build();

        when(usersRepository.existsByEmail("admin@test.ru")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailAndStatus("admin@test.ru", RegistrationStatus.PENDING))
                .thenReturn(false);
        when(universityRepository.findById(1L)).thenReturn(Optional.of(testUniversity));

        assertThrows(BadRequestException.class, () -> registrationService.submitRegistration(request));
        verify(registrationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submit registration with non-existent university throws ResourceNotFoundException")
    void submitRegistrationUniversityNotFound() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@test.ru")
                .password("password123")
                .firstName("Иван")
                .lastName("Иванов")
                .userType(UserType.STUDENT)
                .universityId(999L)
                .groupId(1L)
                .build();

        when(usersRepository.existsByEmail("new@test.ru")).thenReturn(false);
        when(registrationRequestRepository.existsByEmailAndStatus("new@test.ru", RegistrationStatus.PENDING))
                .thenReturn(false);
        when(universityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> registrationService.submitRegistration(request));
    }

    // ── getAllRequests ────────────────────────────────────────────

    @Test
    @DisplayName("Get all requests returns list")
    void getAllRequestsNoFilter() {
        RegistrationRequest rr = RegistrationRequest.builder()
                .id(1L)
                .email("test@test.ru")
                .firstName("Тест")
                .lastName("Тестов")
                .userType(UserType.STUDENT)
                .status(RegistrationStatus.PENDING)
                .university(testUniversity)
                .group(testGroup)
                .build();

        when(registrationRequestRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(rr));

        List<RegistrationRequestResponse> result =
                registrationService.getAllRequests(null, null, null, "admin@uni.ru");
        assertEquals(1, result.size());
        assertEquals("test@test.ru", result.get(0).getEmail());
    }

    @Test
    @DisplayName("Get requests filtered by status")
    void getAllRequestsWithFilter() {
        RegistrationRequest rr = RegistrationRequest.builder()
                .id(1L)
                .email("test@test.ru")
                .firstName("Тест")
                .lastName("Тестов")
                .userType(UserType.STUDENT)
                .status(RegistrationStatus.PENDING)
                .university(testUniversity)
                .build();

        when(registrationRequestRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(rr));

        List<RegistrationRequestResponse> result =
                registrationService.getAllRequests(RegistrationStatus.PENDING, null, null, "admin@uni.ru");
        assertEquals(1, result.size());
        verify(registrationRequestRepository).findAll(any(Specification.class), any(Sort.class));
    }

    // ── approveRequest ───────────────────────────────────────────

    @Test
    @DisplayName("Approve pending request creates user and student profile")
    void approveRequestSuccess() {
        RegistrationRequest regRequest = RegistrationRequest.builder()
                .id(1L)
                .email("approved@test.ru")
                .passwordHash("$2a$10$hashed")
                .firstName("Олег")
                .lastName("Смирнов")
                .userType(UserType.STUDENT)
                .status(RegistrationStatus.PENDING)
                .university(testUniversity)
                .group(testGroup)
                .build();

        Users savedUser = Users.builder()
                .id(10L)
                .email("approved@test.ru")
                .passwordHash("$2a$10$hashed")
                .firstName("Олег")
                .lastName("Смирнов")
                .userType(UserType.STUDENT)
                .isActive(true)
                .build();

        when(registrationRequestRepository.findById(1L)).thenReturn(Optional.of(regRequest));
        when(usersRepository.existsByEmail("approved@test.ru")).thenReturn(false);
        when(usersRepository.save(any(Users.class))).thenReturn(savedUser);

        assertDoesNotThrow(() -> registrationService.approveRequest(1L, "admin@uni.ru"));

        verify(usersRepository).save(any(Users.class));
        verify(studentProfileRepository).save(any(StudentProfile.class));
        verify(registrationRequestRepository).save(regRequest);
        assertEquals(RegistrationStatus.APPROVED, regRequest.getStatus());
        verify(auditService).log(eq(99L), eq("APPROVE_REGISTRATION"), eq("RegistrationRequest"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Approve already approved request throws BadRequestException")
    void approveRequestAlreadyApproved() {
        RegistrationRequest regRequest = RegistrationRequest.builder()
                .id(2L)
                .email("done@test.ru")
                .status(RegistrationStatus.APPROVED)
                .build();

        when(registrationRequestRepository.findById(2L)).thenReturn(Optional.of(regRequest));

        assertThrows(BadRequestException.class, () -> registrationService.approveRequest(2L, "admin@uni.ru"));
        verify(usersRepository, never()).save(any());
    }

    @Test
    @DisplayName("Approve request with email conflict throws ConflictException")
    void approveRequestEmailConflict() {
        RegistrationRequest regRequest = RegistrationRequest.builder()
                .id(3L)
                .email("conflict@test.ru")
                .status(RegistrationStatus.PENDING)
                .university(testUniversity)
                .build();

        when(registrationRequestRepository.findById(3L)).thenReturn(Optional.of(regRequest));
        when(usersRepository.existsByEmail("conflict@test.ru")).thenReturn(true);

        assertThrows(ConflictException.class, () -> registrationService.approveRequest(3L, "admin@uni.ru"));
        verify(usersRepository, never()).save(any());
    }

    @Test
    @DisplayName("Approve non-existent request throws ResourceNotFoundException")
    void approveRequestNotFound() {
        when(registrationRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> registrationService.approveRequest(99L, "admin@uni.ru"));
    }

    // ── rejectRequest ────────────────────────────────────────────

    @Test
    @DisplayName("Reject pending request sets REJECTED status and reason")
    void rejectRequestSuccess() {
        RegistrationRequest regRequest = RegistrationRequest.builder()
                .id(4L)
                .email("rejected@test.ru")
                .status(RegistrationStatus.PENDING)
                .build();

        ApproveRejectRequest rejectBody = ApproveRejectRequest.builder()
                .rejectionReason("Некорректные данные")
                .build();

        when(registrationRequestRepository.findById(4L)).thenReturn(Optional.of(regRequest));

        assertDoesNotThrow(() -> registrationService.rejectRequest(4L, rejectBody, "admin@uni.ru"));

        assertEquals(RegistrationStatus.REJECTED, regRequest.getStatus());
        assertEquals("Некорректные данные", regRequest.getRejectionReason());
        verify(registrationRequestRepository).save(regRequest);
        verify(auditService).log(eq(99L), eq("REJECT_REGISTRATION"), eq("RegistrationRequest"), eq(4L), anyString());
    }

    @Test
    @DisplayName("Reject non-pending request throws BadRequestException")
    void rejectRequestNotPending() {
        RegistrationRequest regRequest = RegistrationRequest.builder()
                .id(5L)
                .email("already@test.ru")
                .status(RegistrationStatus.APPROVED)
                .build();

        ApproveRejectRequest rejectBody = ApproveRejectRequest.builder()
                .rejectionReason("Причина")
                .build();

        when(registrationRequestRepository.findById(5L)).thenReturn(Optional.of(regRequest));

        assertThrows(BadRequestException.class, () -> registrationService.rejectRequest(5L, rejectBody, "admin@uni.ru"));
        verify(registrationRequestRepository, never()).save(any());
    }
}
