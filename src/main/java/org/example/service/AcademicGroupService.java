package org.example.service;

import org.example.dto.request.AcademicGroupRequest;
import org.example.dto.response.AcademicGroupResponse;

import java.util.List;

public interface AcademicGroupService {

    List<AcademicGroupResponse> getAll(Long directionId, Long universityId, String viewerEmail);

    AcademicGroupResponse getById(Long id, String viewerEmail);

    AcademicGroupResponse create(AcademicGroupRequest request, String adminEmail);

    AcademicGroupResponse update(Long id, AcademicGroupRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
