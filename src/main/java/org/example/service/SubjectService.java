package org.example.service;

import org.example.dto.request.SubjectRequest;
import org.example.dto.response.SubjectResponse;

import java.util.List;

public interface SubjectService {

    List<SubjectResponse> getAll();

    SubjectResponse getById(Long id);

    SubjectResponse create(SubjectRequest request);

    SubjectResponse update(Long id, SubjectRequest request);

    void delete(Long id);
}
