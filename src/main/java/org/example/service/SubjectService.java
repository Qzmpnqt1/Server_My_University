package org.example.service;

import org.example.dto.request.SubjectRequest;
import org.example.dto.response.SubjectResponse;

import java.util.List;

public interface SubjectService {

    /**
     * @param requestUniversityId для SUPER_ADMIN: null — все предметы; для ADMIN игнорируется при выводе (берётся кампус).
     * @param viewerEmail       email из Principal или null для гостя/без контекста
     */
    List<SubjectResponse> getAll(Long requestUniversityId, String viewerEmail);

    SubjectResponse getById(Long id);

    SubjectResponse create(SubjectRequest request);

    SubjectResponse update(Long id, SubjectRequest request);

    void delete(Long id);
}
