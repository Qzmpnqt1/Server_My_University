package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectRequest;
import org.example.dto.response.SubjectResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.Subject;
import org.example.repository.SubjectRepository;
import org.example.service.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    public List<SubjectResponse> getAll() {
        return subjectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubjectResponse getById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        return mapToResponse(subject);
    }

    @Override
    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Subject subject = Subject.builder()
                .name(request.getName())
                .build();
        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
        subject.setName(request.getName());
        return mapToResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subject not found with id: " + id);
        }
        subjectRepository.deleteById(id);
    }

    private SubjectResponse mapToResponse(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getId())
                .name(subject.getName())
                .build();
    }
}
