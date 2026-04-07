package org.example.service;

import org.example.dto.response.*;

public interface StatisticsService {

    SubjectStatisticsResponse getSubjectStatistics(Long subjectDirectionId, String viewerEmail, Long groupId);

    PracticeStatisticsResponse getPracticeStatistics(Long subjectDirectionId, String viewerEmail, Long groupId);

    GroupStatisticsResponse getGroupStatistics(Long groupId, String viewerEmail);

    DirectionStatisticsResponse getDirectionStatistics(Long directionId, String viewerEmail);

    InstituteStatisticsResponse getInstituteStatistics(Long instituteId, String viewerEmail);

    UniversityStatisticsResponse getUniversityStatistics(Long universityId, String viewerEmail);

    ScheduleStatisticsResponse getTeacherScheduleStatistics(Long teacherId, String viewerEmail, Integer weekNumber);

    ScheduleStatisticsResponse getGroupScheduleStatistics(Long groupId, String viewerEmail, Integer weekNumber);

    ScheduleStatisticsResponse getClassroomScheduleStatistics(Long classroomId, String viewerEmail, Integer weekNumber);

    StudentPerformanceSummaryResponse getMyStudentPerformanceSummary(String email, Integer course, Integer semester);
}
