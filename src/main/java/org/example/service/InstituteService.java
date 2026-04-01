package org.example.service;

import org.example.dto.request.InstituteRequest;
import org.example.dto.response.InstituteResponse;

import java.util.List;

public interface InstituteService {

    List<InstituteResponse> getAll(Long universityId, String viewerEmail);

    InstituteResponse getById(Long id, String viewerEmail);

    InstituteResponse create(InstituteRequest request, String adminEmail);

    InstituteResponse update(Long id, InstituteRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
