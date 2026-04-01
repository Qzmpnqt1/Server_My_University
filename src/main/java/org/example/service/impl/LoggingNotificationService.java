package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.service.NotificationService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingNotificationService implements NotificationService {

    @Override
    public void notifyRegistrationApproved(String userEmail) {
        log.info("[NOTIFY] Регистрация подтверждена: {}", userEmail);
    }

    @Override
    public void notifyRegistrationRejected(String userEmail, String reason) {
        log.info("[NOTIFY] Регистрация отклонена: {} — {}", userEmail, reason);
    }

    @Override
    public void notifyGradeChanged(Long studentUserId, String subjectName, boolean practice) {
        log.info("[NOTIFY] Оценка изменена: studentUserId={}, subject={}, practice={}",
                studentUserId, subjectName, practice);
    }

    @Override
    public void notifyScheduleChanged(Long groupId, Long teacherUserId) {
        log.info("[NOTIFY] Расписание изменено: groupId={}, teacherUserId={}", groupId, teacherUserId);
    }

    @Override
    public void notifyNewChatMessage(Long recipientUserId, String senderName) {
        log.info("[NOTIFY] Новое сообщение: recipientUserId={}, от {}", recipientUserId, senderName);
    }
}
