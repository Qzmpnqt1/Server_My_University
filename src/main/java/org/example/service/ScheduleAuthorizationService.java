package org.example.service;

public interface ScheduleAuthorizationService {

    void ensureAdmin(String email);

    void ensureCanViewGroupSchedule(String email, Long groupId);

    void ensureCanViewTeacherSchedule(String email, Long teacherUserId);

    void ensureCanViewScheduleEntry(String email, Long scheduleId);
}
