package org.example.service;

import org.example.dto.request.SubjectInDirectionRequest;
import org.example.dto.response.SubjectInDirectionResponse;

import java.util.List;

public interface SubjectInDirectionService {

    /**
     * @param universityId для SUPER_ADMIN без directionId: null — все вузы, иначе фильтр по вузу; для ADMIN игнорируется при выборке по вузу кампуса.
     */
    List<SubjectInDirectionResponse> getAll(Long directionId, Long universityId, String viewerEmail);

    SubjectInDirectionResponse getById(Long id, String viewerEmail);

    SubjectInDirectionResponse create(SubjectInDirectionRequest request, String adminEmail);

    SubjectInDirectionResponse update(Long id, SubjectInDirectionRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
