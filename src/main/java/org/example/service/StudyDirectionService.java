package org.example.service;

import org.example.dto.request.StudyDirectionRequest;
import org.example.dto.response.StudyDirectionResponse;

import java.util.List;

public interface StudyDirectionService {

    List<StudyDirectionResponse> getAll(Long instituteId, String viewerEmail);

    StudyDirectionResponse getById(Long id, String viewerEmail);

    StudyDirectionResponse create(StudyDirectionRequest request, String adminEmail);

    StudyDirectionResponse update(Long id, StudyDirectionRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
