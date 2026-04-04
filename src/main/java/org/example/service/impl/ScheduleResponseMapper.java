package org.example.service.impl;

import org.example.dto.response.ScheduleResponse;
import org.example.model.*;

/**
 * Единое преобразование сущности расписания в DTO (используется ScheduleServiceImpl и сравнение).
 */
public final class ScheduleResponseMapper {

    private ScheduleResponseMapper() {
    }

    public static ScheduleResponse toResponse(Schedule schedule) {
        SubjectLessonType slt = schedule.getSubjectType();
        SubjectInDirection sid = slt.getSubjectDirection();
        Users teacher = schedule.getTeacher();
        AcademicGroup group = schedule.getGroup();
        Classroom classroom = schedule.getClassroom();

        String teacherName = teacher.getLastName() + " " + teacher.getFirstName();
        if (teacher.getMiddleName() != null && !teacher.getMiddleName().isEmpty()) {
            teacherName += " " + teacher.getMiddleName();
        }

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .subjectTypeId(slt.getId())
                .subjectName(sid.getSubject().getName())
                .lessonType(slt.getLessonType())
                .teacherId(teacher.getId())
                .teacherName(teacherName)
                .groupId(group.getId())
                .groupName(group.getName())
                .classroomId(classroom.getId())
                .classroomInfo(classroom.getBuilding() + ", ауд. " + classroom.getRoomNumber())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .weekNumber(schedule.getWeekNumber())
                .build();
    }
}
