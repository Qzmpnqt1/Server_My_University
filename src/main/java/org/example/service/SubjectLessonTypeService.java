package org.example.service;

import org.example.dto.request.SubjectLessonTypeRequest;
import org.example.dto.response.SubjectLessonTypeResponse;

import java.util.List;

public interface SubjectLessonTypeService {

    List<SubjectLessonTypeResponse> getAll(Long subjectDirectionId, String viewerEmail);

    SubjectLessonTypeResponse create(SubjectLessonTypeRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
