package org.example.service;

import org.example.model.StudentProfile;
import org.example.model.Users;
import org.example.repository.InAppNotificationRepository;
import org.example.repository.StudentProfileRepository;
import org.example.repository.UsersRepository;
import org.example.service.impl.PersistentNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistentNotificationServiceTest {

    @Mock
    private InAppNotificationRepository inAppNotificationRepository;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private PersistentNotificationService notificationService;

    private Users student;
    private Users teacher;

    @BeforeEach
    void setUp() {
        student = Users.builder().id(10L).email("s@t.ru").build();
        teacher = Users.builder().id(20L).email("t@t.ru").build();
    }

    @Test
    @DisplayName("notifyGradeChanged сохраняет уведомление студенту")
    void gradeChangedPersists() {
        when(usersRepository.findById(10L)).thenReturn(Optional.of(student));

        notificationService.notifyGradeChanged(10L, "Математика", false);

        verify(inAppNotificationRepository).save(any());
    }

    @Test
    @DisplayName("notifyScheduleChanged — запись для каждого студента группы и преподавателя")
    void scheduleFanOut() {
        Users s1 = Users.builder().id(1L).build();
        Users s2 = Users.builder().id(2L).build();
        when(studentProfileRepository.findByGroupId(100L)).thenReturn(List.of(
                StudentProfile.builder().user(s1).build(),
                StudentProfile.builder().user(s2).build()
        ));
        when(usersRepository.findById(1L)).thenReturn(Optional.of(s1));
        when(usersRepository.findById(2L)).thenReturn(Optional.of(s2));
        when(usersRepository.findById(20L)).thenReturn(Optional.of(teacher));

        notificationService.notifyScheduleChanged(100L, 20L);

        var cap = ArgumentCaptor.forClass(org.example.model.InAppNotification.class);
        verify(inAppNotificationRepository, times(3)).save(cap.capture());
    }

    @Test
    @DisplayName("notifyRegistrationApproved — после создания пользователя")
    void registrationApproved() {
        when(usersRepository.findByEmail("new@t.ru")).thenReturn(Optional.of(student));
        notificationService.notifyRegistrationApproved("new@t.ru");
        verify(inAppNotificationRepository).save(any());
    }
}
