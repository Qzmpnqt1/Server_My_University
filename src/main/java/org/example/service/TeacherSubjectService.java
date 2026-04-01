package org.example.service;

import org.example.dto.request.TeacherSubjectRequest;
import org.example.dto.response.TeacherSubjectResponse;

import java.util.List;

public interface TeacherSubjectService {

    List<TeacherSubjectResponse> getAll(Long teacherId, String viewerEmail);

    TeacherSubjectResponse create(TeacherSubjectRequest request, String adminEmail);

    void delete(Long id, String adminEmail);
}
