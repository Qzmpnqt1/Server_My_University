package org.example.service;

import org.example.dto.request.SubjectPracticeRequest;
import org.example.dto.response.SubjectPracticeResponse;

import java.util.List;

public interface SubjectPracticeService {

    List<SubjectPracticeResponse> getBySubjectDirection(Long subjectDirectionId, String viewerEmail);

    SubjectPracticeResponse getById(Long id, String viewerEmail);

    SubjectPracticeResponse create(SubjectPracticeRequest request, String adminEmail);

    SubjectPracticeResponse update(Long id, SubjectPracticeRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
