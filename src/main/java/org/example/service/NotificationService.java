package org.example.service;

/**
 * Точка расширения для push/e-mail уведомлений. Сейчас фиксирует события в логах.
 */
public interface NotificationService {

    void notifyRegistrationApproved(String userEmail);

    void notifyRegistrationRejected(String userEmail, String reason);

    void notifyGradeChanged(Long studentUserId, String subjectName, boolean practice);

    void notifyScheduleChanged(Long groupId, Long teacherUserId);

    void notifyNewChatMessage(Long recipientUserId, String senderName);
}
