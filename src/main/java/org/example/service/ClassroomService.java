package org.example.service;

import org.example.dto.request.ClassroomRequest;
import org.example.dto.response.ClassroomResponse;

import java.util.List;

public interface ClassroomService {

    List<ClassroomResponse> getAll(Long universityId, String viewerEmail);

    ClassroomResponse getById(Long id, String viewerEmail);

    ClassroomResponse create(ClassroomRequest request, String adminEmail);

    ClassroomResponse update(Long id, ClassroomRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
