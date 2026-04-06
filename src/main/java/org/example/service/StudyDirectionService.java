package org.example.service;

import org.example.dto.request.StudyDirectionRequest;
import org.example.dto.response.StudyDirectionResponse;

import java.util.List;

public interface StudyDirectionService {

    /**
     * @param universityId для SUPER_ADMIN без instituteId: фильтр направлений по вузу; null — все направления.
     *                     для ADMIN игнорируется (всегда кампусный вуз), кроме проверки совпадения если передан.
     */
    List<StudyDirectionResponse> getAll(Long instituteId, Long universityId, String viewerEmail);

    StudyDirectionResponse getById(Long id, String viewerEmail);

    StudyDirectionResponse create(StudyDirectionRequest request, String adminEmail);

    StudyDirectionResponse update(Long id, StudyDirectionRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
