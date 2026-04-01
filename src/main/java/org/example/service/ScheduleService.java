package org.example.service;

import org.example.dto.request.ScheduleRequest;
import org.example.dto.response.ScheduleResponse;

import java.util.List;

public interface ScheduleService {

    List<ScheduleResponse> getAllForAdmin(String adminEmail);

    ScheduleResponse getById(Long id);

    List<ScheduleResponse> getByGroup(Long groupId, Integer weekNumber, Integer dayOfWeek);

    List<ScheduleResponse> getByTeacher(Long teacherId, Integer weekNumber, Integer dayOfWeek);

    List<ScheduleResponse> getMySchedule(String email, Integer weekNumber, Integer dayOfWeek);

    /**
     * Расписание всех групп, с которыми преподаватель связан (через schedule и/или закреплённые дисциплины).
     */
    List<ScheduleResponse> getLinkedGroupsSchedule(String teacherEmail, Integer weekNumber, Integer dayOfWeek);

    ScheduleResponse create(ScheduleRequest request, String actorEmail);

    ScheduleResponse update(Long id, ScheduleRequest request, String actorEmail);

    void delete(Long id, String actorEmail);
}
