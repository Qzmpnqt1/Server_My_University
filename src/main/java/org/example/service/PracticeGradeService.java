package org.example.service;

import org.example.dto.request.PracticeGradeRequest;
import org.example.dto.response.PracticeGradeResponse;
import org.example.dto.response.StudentPracticeSlotResponse;

import java.util.List;

public interface PracticeGradeService {

    List<PracticeGradeResponse> getMyPracticeGrades(String email, Long subjectDirectionId);

    /**
     * Полный список практик дисциплины для студента с учётом выставленных оценок (если есть).
     */
    List<StudentPracticeSlotResponse> getMyPracticeSlotsForSubject(String email, Long subjectDirectionId);

    List<PracticeGradeResponse> getByPractice(Long practiceId, String email);

    PracticeGradeResponse create(PracticeGradeRequest request, String email);

    PracticeGradeResponse update(Long id, PracticeGradeRequest request, String email);
}
