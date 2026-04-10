package org.example.service.impl;

import org.example.dto.request.TeacherSubjectReplaceRequest;
import org.example.dto.request.TeacherSubjectRequest;
import org.example.dto.response.TeacherSubjectResponse;
import org.example.exception.ConflictException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.SubjectInDirectionRepository;
import org.example.repository.TeacherProfileRepository;
import org.example.repository.TeacherSubjectRepository;
import org.example.repository.UsersRepository;
import org.example.service.UniversityScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherSubjectServiceImplTest {

    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;
    @Mock
    private TeacherProfileRepository teacherProfileRepository;
    @Mock
    private SubjectInDirectionRepository subjectInDirectionRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private UniversityScopeService universityScopeService;

    private TeacherSubjectServiceImpl service;

    private University university;
    private Institute institute;
    private StudyDirection direction;
    private Subject subject;
    private SubjectInDirection sid;
    private Users teacherUser;
    private TeacherProfile teacherProfile;
    private TeacherSubject assignment;

    @BeforeEach
    void setUp() {
        service = new TeacherSubjectServiceImpl(
                teacherSubjectRepository,
                teacherProfileRepository,
                subjectInDirectionRepository,
                usersRepository,
                universityScopeService
        );
        lenient().when(universityScopeService.enforceAccessToEntityUniversity(anyString(), anyLong())).thenReturn(1L);
        lenient().doNothing().when(universityScopeService).assertUserInUniversity(anyLong(), anyLong());
        lenient().doNothing().when(universityScopeService).assertSubjectDirectionInUniversity(anyLong(), anyLong());

        university = University.builder().id(1L).name("U").build();
        institute = Institute.builder().id(2L).name("I").university(university).build();
        direction = StudyDirection.builder().id(3L).name("D").institute(institute).build();
        subject = Subject.builder().id(4L).name("Math").build();
        sid = SubjectInDirection.builder()
                .id(100L)
                .direction(direction)
                .subject(subject)
                .course(1)
                .semester(1)
                .build();
        teacherUser = Users.builder()
                .id(50L)
                .email("t@u.ru")
                .firstName("Иван")
                .lastName("Петров")
                .userType(UserType.TEACHER)
                .build();
        teacherProfile = TeacherProfile.builder()
                .id(10L)
                .user(teacherUser)
                .university(university)
                .build();
        assignment = TeacherSubject.builder()
                .id(200L)
                .teacher(teacherProfile)
                .subjectInDirection(sid)
                .build();
    }

    @Test
    void getAll_anonymousViewer_returnsAllOrByTeacher() {
        when(teacherSubjectRepository.findAll()).thenReturn(List.of(assignment));

        List<TeacherSubjectResponse> all = service.getAll(null, null);
        assertEquals(1, all.size());
        assertEquals(200L, all.get(0).getId());

        when(teacherSubjectRepository.findByTeacherId(10L)).thenReturn(List.of(assignment));
        List<TeacherSubjectResponse> byTeacher = service.getAll(10L, "");
        assertEquals(1, byTeacher.size());
    }

    @Test
    void getAll_adminScoped_filtersByCampus() {
        Users admin = Users.builder().id(1L).email("a@u.ru").userType(UserType.ADMIN).build();
        when(usersRepository.findByEmail("a@u.ru")).thenReturn(Optional.of(admin));
        when(universityScopeService.requireCampusUniversityId("a@u.ru")).thenReturn(1L);
        when(teacherSubjectRepository.findByTeacherInUniversityId(1L)).thenReturn(List.of(assignment));

        List<TeacherSubjectResponse> res = service.getAll(null, "a@u.ru");
        assertEquals(1, res.size());
        verify(teacherSubjectRepository).findByTeacherInUniversityId(1L);
    }

    @Test
    void create_success_saves() {
        TeacherSubjectRequest req = TeacherSubjectRequest.builder()
                .teacherId(10L)
                .subjectDirectionId(100L)
                .build();
        when(teacherProfileRepository.findById(10L)).thenReturn(Optional.of(teacherProfile));
        when(subjectInDirectionRepository.findById(100L)).thenReturn(Optional.of(sid));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 100L)).thenReturn(false);
        when(teacherSubjectRepository.save(any(TeacherSubject.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherSubjectResponse out = service.create(req, "admin@u.ru");

        assertNotNull(out);
        assertEquals(10L, out.getTeacherId());
        verify(universityScopeService).enforceAccessToEntityUniversity(eq("admin@u.ru"), eq(1L));
        verify(teacherSubjectRepository).save(any(TeacherSubject.class));
    }

    @Test
    void create_duplicate_throwsConflict() {
        TeacherSubjectRequest req = TeacherSubjectRequest.builder()
                .teacherId(10L)
                .subjectDirectionId(100L)
                .build();
        when(teacherProfileRepository.findById(10L)).thenReturn(Optional.of(teacherProfile));
        when(subjectInDirectionRepository.findById(100L)).thenReturn(Optional.of(sid));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectInDirection_Id(10L, 100L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(req, "admin@u.ru"));
    }

    @Test
    void delete_success_deletes() {
        when(teacherSubjectRepository.findById(200L)).thenReturn(Optional.of(assignment));

        service.delete(200L, "admin@u.ru");

        verify(universityScopeService).enforceAccessToEntityUniversity(eq("admin@u.ru"), eq(1L));
        verify(teacherSubjectRepository).deleteById(200L);
    }

    @Test
    void delete_missing_throws() {
        when(teacherSubjectRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(999L, "admin@u.ru"));
    }

    @Test
    void replaceAssignments_expectedCountMismatch_throws() {
        TeacherSubjectReplaceRequest req = TeacherSubjectReplaceRequest.builder()
                .subjectDirectionIds(List.of())
                .expectedAssignmentCount(2)
                .build();
        when(teacherProfileRepository.findById(10L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.findByTeacherId(10L)).thenReturn(List.of(assignment));

        assertThrows(ConflictException.class, () -> service.replaceAssignments(10L, req, "admin@u.ru"));
    }

    @Test
    void replaceAssignments_clearsAndAdds() {
        TeacherSubjectReplaceRequest req = TeacherSubjectReplaceRequest.builder()
                .subjectDirectionIds(List.of(100L))
                .build();
        when(teacherProfileRepository.findById(10L)).thenReturn(Optional.of(teacherProfile));
        when(teacherSubjectRepository.findByTeacherId(10L)).thenReturn(List.of())
                .thenReturn(List.of(assignment));
        when(subjectInDirectionRepository.existsById(100L)).thenReturn(true);
        when(subjectInDirectionRepository.findById(100L)).thenReturn(Optional.of(sid));
        when(teacherSubjectRepository.save(any(TeacherSubject.class))).thenAnswer(inv -> inv.getArgument(0));

        List<TeacherSubjectResponse> out = service.replaceAssignments(10L, req, "admin@u.ru");

        assertFalse(out.isEmpty());
        verify(teacherSubjectRepository).save(any(TeacherSubject.class));
        verify(teacherSubjectRepository).flush();
    }
}
