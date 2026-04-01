package org.example.service;

import org.example.dto.response.*;

public interface StatisticsService {

    SubjectStatisticsResponse getSubjectStatistics(Long subjectDirectionId, String viewerEmail);

    PracticeStatisticsResponse getPracticeStatistics(Long subjectDirectionId, String viewerEmail);

    GroupStatisticsResponse getGroupStatistics(Long groupId, String viewerEmail);

    DirectionStatisticsResponse getDirectionStatistics(Long directionId, String viewerEmail);

    InstituteStatisticsResponse getInstituteStatistics(Long instituteId, String viewerEmail);

    UniversityStatisticsResponse getUniversityStatistics(Long universityId, String viewerEmail);

    ScheduleStatisticsResponse getTeacherScheduleStatistics(Long teacherId, String viewerEmail);

    ScheduleStatisticsResponse getGroupScheduleStatistics(Long groupId, String viewerEmail);

    ScheduleStatisticsResponse getClassroomScheduleStatistics(Long classroomId, String viewerEmail);

    StudentPerformanceSummaryResponse getMyStudentPerformanceSummary(String email, Integer course, Integer semester);
}
