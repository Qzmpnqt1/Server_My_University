package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.InAppNotification;
import org.example.model.Users;
import org.example.repository.InAppNotificationRepository;
import org.example.repository.StudentProfileRepository;
import org.example.repository.UsersRepository;
import org.example.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Уведомления о ключевых событиях: запись в БД для in-app ленты на клиенте + логирование для сопровождения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersistentNotificationService implements NotificationService {

    public static final String KIND_REGISTRATION_APPROVED = "REGISTRATION_APPROVED";
    public static final String KIND_REGISTRATION_REJECTED = "REGISTRATION_REJECTED";
    public static final String KIND_GRADE_CHANGED = "GRADE_CHANGED";
    public static final String KIND_SCHEDULE_CHANGED = "SCHEDULE_CHANGED";
    public static final String KIND_CHAT_MESSAGE = "CHAT_MESSAGE";

    private final InAppNotificationRepository inAppNotificationRepository;
    private final UsersRepository usersRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Override
    @Transactional
    public void notifyRegistrationApproved(String userEmail) {
        log.info("[NOTIFY] Регистрация подтверждена: {}", userEmail);
        usersRepository.findByEmail(userEmail).ifPresent(u -> persist(
                u,
                KIND_REGISTRATION_APPROVED,
                "Заявка одобрена",
                "Ваша заявка на регистрацию подтверждена. Можно войти в приложение."));
    }

    @Override
    public void notifyRegistrationRejected(String userEmail, String reason) {
        log.info("[NOTIFY] Регистрация отклонена: {} — {}", userEmail, reason);
        // У отклонённого гостя ещё нет учётной записи — в ленту не пишем.
    }

    @Override
    @Transactional
    public void notifyGradeChanged(Long studentUserId, String subjectName, boolean practice) {
        log.info("[NOTIFY] Оценка изменена: studentUserId={}, subject={}, practice={}",
                studentUserId, subjectName, practice);
        String title = practice ? "Оценка за практику" : "Итоговая оценка";
        String body = practice
                ? ("Обновлена оценка за практику по дисциплине: " + subjectName)
                : ("Обновлена итоговая оценка по дисциплине: " + subjectName);
        saveByUserId(studentUserId, KIND_GRADE_CHANGED, title, body);
    }

    @Override
    @Transactional
    public void notifyScheduleChanged(Long groupId, Long teacherUserId) {
        log.info("[NOTIFY] Расписание изменено: groupId={}, teacherUserId={}", groupId, teacherUserId);
        String title = "Расписание обновлено";
        String body = "Изменено расписание занятий, затрагивающее вашу группу или преподавателя.";
        studentProfileRepository.findByGroupId(groupId).forEach(sp ->
                saveByUserId(sp.getUser().getId(), KIND_SCHEDULE_CHANGED, title, body));
        saveByUserId(teacherUserId, KIND_SCHEDULE_CHANGED, title, body);
    }

    @Override
    @Transactional
    public void notifyNewChatMessage(Long recipientUserId, String senderName) {
        log.info("[NOTIFY] Новое сообщение: recipientUserId={}, от {}", recipientUserId, senderName);
        saveByUserId(recipientUserId, KIND_CHAT_MESSAGE,
                "Новое сообщение",
                "Сообщение от " + senderName);
    }

    private void saveByUserId(Long userId, String kind, String title, String body) {
        if (userId == null) {
            return;
        }
        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("notify: пользователь id={} не найден, уведомление не сохранено", userId);
            return;
        }
        persist(user, kind, title, body);
    }

    private void persist(Users user, String kind, String title, String body) {
        inAppNotificationRepository.save(InAppNotification.builder()
                .user(user)
                .kind(kind)
                .title(title)
                .body(body)
                .build());
    }
}
