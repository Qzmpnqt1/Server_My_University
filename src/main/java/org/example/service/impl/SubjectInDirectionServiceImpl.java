package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectInDirectionRequest;
import org.example.dto.response.SubjectInDirectionResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.StudyDirectionRepository;
import org.example.repository.SubjectInDirectionRepository;
import org.example.repository.SubjectRepository;
import org.example.repository.UsersRepository;
import org.example.service.SubjectInDirectionService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectInDirectionServiceImpl implements SubjectInDirectionService {

    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudyDirectionRepository studyDirectionRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<SubjectInDirectionResponse> getAll(Long directionId, String viewerEmail) {
        return listEntities(directionId, viewerEmail).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private List<SubjectInDirection> listEntities(Long directionId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent() && viewer.get().getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
            if (directionId != null) {
                universityScopeService.assertStudyDirectionInUniversity(directionId, uni);
                return subjectInDirectionRepository.findByDirectionId(directionId);
            }
            return subjectInDirectionRepository.findByDirection_Institute_University_Id(uni);
        }
        if (directionId != null) {
            return subjectInDirectionRepository.findByDirectionId(directionId);
        }
        return subjectInDirectionRepository.findAll();
    }

    @Override
    public SubjectInDirectionResponse getById(Long id, String viewerEmail) {
        SubjectInDirection entity = subjectInDirectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + id));
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
                universityScopeService.assertSubjectDirectionInUniversity(id, uni);
            }
        });
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public SubjectInDirectionResponse create(SubjectInDirectionRequest request, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertStudyDirectionInUniversity(request.getDirectionId(), uni);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
        StudyDirection direction = studyDirectionRepository.findById(request.getDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + request.getDirectionId()));
        FinalAssessmentType fat = request.getFinalAssessmentType() != null
                ? request.getFinalAssessmentType()
                : FinalAssessmentType.EXAM;
        SubjectInDirection entity = SubjectInDirection.builder()
                .subject(subject)
                .direction(direction)
                .course(request.getCourse())
                .semester(request.getSemester())
                .finalAssessmentType(fat)
                .build();
        return mapToResponse(subjectInDirectionRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectInDirectionResponse update(Long id, SubjectInDirectionRequest request, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        SubjectInDirection entity = subjectInDirectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + id));
        universityScopeService.assertSubjectDirectionInUniversity(id, uni);
        universityScopeService.assertStudyDirectionInUniversity(request.getDirectionId(), uni);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
        StudyDirection direction = studyDirectionRepository.findById(request.getDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + request.getDirectionId()));
        entity.setSubject(subject);
        entity.setDirection(direction);
        entity.setCourse(request.getCourse());
        entity.setSemester(request.getSemester());
        if (request.getFinalAssessmentType() != null) {
            entity.setFinalAssessmentType(request.getFinalAssessmentType());
        }
        return mapToResponse(subjectInDirectionRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertSubjectDirectionInUniversity(id, uni);
        if (!subjectInDirectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("SubjectInDirection not found with id: " + id);
        }
        subjectInDirectionRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private SubjectInDirectionResponse mapToResponse(SubjectInDirection entity) {
        return SubjectInDirectionResponse.builder()
                .id(entity.getId())
                .subjectId(entity.getSubject().getId())
                .subjectName(entity.getSubject().getName())
                .directionId(entity.getDirection().getId())
                .directionName(entity.getDirection().getName())
                .course(entity.getCourse())
                .semester(entity.getSemester())
                .finalAssessmentType(entity.getFinalAssessmentType())
                .build();
    }
}
