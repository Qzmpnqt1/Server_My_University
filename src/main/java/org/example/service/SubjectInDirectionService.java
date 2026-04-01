package org.example.service;

import org.example.dto.request.SubjectInDirectionRequest;
import org.example.dto.response.SubjectInDirectionResponse;

import java.util.List;

public interface SubjectInDirectionService {

    List<SubjectInDirectionResponse> getAll(Long directionId, String viewerEmail);

    SubjectInDirectionResponse getById(Long id, String viewerEmail);

    SubjectInDirectionResponse create(SubjectInDirectionRequest request, String adminEmail);

    SubjectInDirectionResponse update(Long id, SubjectInDirectionRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
