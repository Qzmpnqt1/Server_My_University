package org.example.service;

import org.example.dto.request.GradeRequest;
import org.example.dto.response.GradeResponse;
import org.example.dto.response.TeacherJournalResponse;

import java.util.List;

public interface GradeService {

    List<GradeResponse> getMyGrades(String email);

    List<GradeResponse> getByStudent(Long studentId, String email);

    List<GradeResponse> getBySubjectDirection(Long subjectDirectionId, String email);

    TeacherJournalResponse getTeacherJournal(Long subjectDirectionId, String email);

    GradeResponse create(GradeRequest request, String email);

    GradeResponse update(Long id, GradeRequest request, String email);
}
