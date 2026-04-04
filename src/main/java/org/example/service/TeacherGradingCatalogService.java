package org.example.service;

import org.example.dto.response.*;

import java.util.List;

public interface TeacherGradingCatalogService {

    List<TeacherGradingPickResponse> listInstitutes(String teacherEmail);

    List<TeacherGradingPickResponse> listDirections(String teacherEmail, Long instituteId);

    List<SubjectInDirectionResponse> listSubjectDirections(String teacherEmail, Long directionId);

    List<TeacherGradingPickResponse> listGroups(String teacherEmail, Long subjectDirectionId);

    List<TeacherGradingPickResponse> listStudents(String teacherEmail, Long subjectDirectionId, Long groupId);

    TeacherStudentAssessmentResponse getAssessment(String teacherEmail, Long subjectDirectionId,
                                                   Long groupId, Long studentUserId);
}
