package org.example.service;

import org.example.dto.request.UniversityRequest;
import org.example.dto.response.UniversityResponse;

import java.util.List;

public interface UniversityService {

    List<UniversityResponse> getAll(String viewerEmail);

    UniversityResponse getById(Long id, String viewerEmail);

    UniversityResponse create(UniversityRequest request, String adminEmail);

    UniversityResponse update(Long id, UniversityRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
