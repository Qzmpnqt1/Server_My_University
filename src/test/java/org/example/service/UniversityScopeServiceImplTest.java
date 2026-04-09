package org.example.service;

import org.example.exception.AccessDeniedException;
import org.example.model.AdminProfile;
import org.example.model.Institute;
import org.example.model.StudentProfile;
import org.example.model.University;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.AcademicGroupRepository;
import org.example.repository.AdminProfileRepository;
import org.example.repository.ClassroomRepository;
import org.example.repository.InstituteRepository;
import org.example.repository.RegistrationRequestRepository;
import org.example.repository.ScheduleRepository;
import org.example.repository.StudentProfileRepository;
import org.example.repository.StudyDirectionRepository;
import org.example.repository.SubjectInDirectionRepository;
import org.example.repository.TeacherProfileRepository;
import org.example.repository.UsersRepository;
import org.example.service.impl.UniversityScopeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityScopeServiceImplTest {

    @Mock
    UsersRepository usersRepository;
    @Mock
    AdminProfileRepository adminProfileRepository;
    @Mock
    StudentProfileRepository studentProfileRepository;
    @Mock
    TeacherProfileRepository teacherProfileRepository;
    @Mock
    InstituteRepository instituteRepository;
    @Mock
    StudyDirectionRepository studyDirectionRepository;
    @Mock
    AcademicGroupRepository academicGroupRepository;
    @Mock
    ClassroomRepository classroomRepository;
    @Mock
    SubjectInDirectionRepository subjectInDirectionRepository;
    @Mock
    RegistrationRequestRepository registrationRequestRepository;
    @Mock
    ScheduleRepository scheduleRepository;

    @InjectMocks
    UniversityScopeServiceImpl service;

    @Test
    @DisplayName("ADMIN: scope ограничен своим вузом")
    void resolveAdminQueryScope_admin_returnsCampus() {
        Users u = mock(Users.class);
        when(u.getId()).thenReturn(10L);
        when(u.getUserType()).thenReturn(UserType.ADMIN);
        when(usersRepository.findByEmail("admin@x.ru")).thenReturn(Optional.of(u));

        University uni = mock(University.class);
        when(uni.getId()).thenReturn(7L);
        AdminProfile ap = mock(AdminProfile.class);
        when(ap.getUniversity()).thenReturn(uni);
        when(adminProfileRepository.findFetchedByUserId(10L)).thenReturn(Optional.of(ap));

        UniversityScopeService.AdminQueryScope scope = service.resolveAdminQueryScope("admin@x.ru", null);
        assertThat(scope.globalAllUniversities()).isFalse();
        assertThat(scope.universityId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("ADMIN: запрос чужого вуза — AccessDenied")
    void resolveAdminQueryScope_admin_otherUniversity_denied() {
        Users u = mock(Users.class);
        when(u.getId()).thenReturn(10L);
        when(u.getUserType()).thenReturn(UserType.ADMIN);
        when(usersRepository.findByEmail("admin@x.ru")).thenReturn(Optional.of(u));

        University uni = mock(University.class);
        when(uni.getId()).thenReturn(7L);
        AdminProfile ap = mock(AdminProfile.class);
        when(ap.getUniversity()).thenReturn(uni);
        when(adminProfileRepository.findFetchedByUserId(10L)).thenReturn(Optional.of(ap));

        assertThatThrownBy(() -> service.resolveAdminQueryScope("admin@x.ru", 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SUPER_ADMIN без фильтра — глобальный scope")
    void resolveAdminQueryScope_superAdmin_nullRequest_allUniversities() {
        Users u = mock(Users.class);
        when(u.getUserType()).thenReturn(UserType.SUPER_ADMIN);
        when(usersRepository.findByEmail("super@x.ru")).thenReturn(Optional.of(u));

        UniversityScopeService.AdminQueryScope scope = service.resolveAdminQueryScope("super@x.ru", null);
        assertThat(scope.globalAllUniversities()).isTrue();
        assertThat(scope.universityId()).isNull();
    }

    @Test
    @DisplayName("SUPER_ADMIN с universityId — ограничение выбранным вузом")
    void resolveAdminQueryScope_superAdmin_withUniversity_filters() {
        Users u = mock(Users.class);
        when(u.getUserType()).thenReturn(UserType.SUPER_ADMIN);
        when(usersRepository.findByEmail("super@x.ru")).thenReturn(Optional.of(u));

        UniversityScopeService.AdminQueryScope scope = service.resolveAdminQueryScope("super@x.ru", 3L);
        assertThat(scope.globalAllUniversities()).isFalse();
        assertThat(scope.universityId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("STUDENT не может resolveAdminQueryScope")
    void resolveAdminQueryScope_student_denied() {
        Users u = mock(Users.class);
        when(u.getUserType()).thenReturn(UserType.STUDENT);
        when(usersRepository.findByEmail("st@x.ru")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.resolveAdminQueryScope("st@x.ru", null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("isSuperAdmin по email")
    void isSuperAdmin() {
        Users u = mock(Users.class);
        when(u.getUserType()).thenReturn(UserType.SUPER_ADMIN);
        when(usersRepository.findByEmail("s@x.ru")).thenReturn(Optional.of(u));
        assertThat(service.isSuperAdmin("s@x.ru")).isTrue();

        Users t = mock(Users.class);
        when(t.getUserType()).thenReturn(UserType.TEACHER);
        when(usersRepository.findByEmail("t@x.ru")).thenReturn(Optional.of(t));
        assertThat(service.isSuperAdmin("t@x.ru")).isFalse();
    }

    @Test
    @DisplayName("userBelongsToUniversity: студент по институту вуза")
    void userBelongsToUniversity_student() {
        Users user = mock(Users.class);
        when(user.getUserType()).thenReturn(UserType.STUDENT);
        when(usersRepository.findById(1L)).thenReturn(Optional.of(user));

        University uni = mock(University.class);
        when(uni.getId()).thenReturn(5L);
        Institute inst = mock(Institute.class);
        when(inst.getUniversity()).thenReturn(uni);
        StudentProfile sp = mock(StudentProfile.class);
        when(sp.getInstitute()).thenReturn(inst);
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(sp));

        assertThat(service.userBelongsToUniversity(1L, 5L)).isTrue();
        assertThat(service.userBelongsToUniversity(1L, 6L)).isFalse();
    }
}
