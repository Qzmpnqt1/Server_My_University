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
import org.example.service.UniversityScopeService.AdminQueryScope;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public List<SubjectInDirectionResponse> getAll(Long directionId, Long universityId, String viewerEmail) {
        List<SubjectInDirection> entities = new ArrayList<>(listEntities(directionId, universityId, viewerEmail));
        RussianSort.sortSubjectInDirectionEntities(entities);
        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private List<SubjectInDirection> listEntities(Long directionId, Long universityId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent()) {
            UserType t = viewer.get().getUserType();
            if (t == UserType.ADMIN) {
                Long uni = universityScopeService.requireCampusUniversityId(viewerEmail);
                if (directionId != null) {
                    universityScopeService.assertStudyDirectionInUniversity(directionId, uni);
                    return subjectInDirectionRepository.findByDirectionId(directionId);
                }
                return subjectInDirectionRepository.findByDirection_Institute_University_Id(uni);
            }
            if (t == UserType.SUPER_ADMIN) {
                if (directionId != null) {
                    StudyDirection d = studyDirectionRepository.findById(directionId)
                            .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + directionId));
                    universityScopeService.enforceAccessToEntityUniversity(viewerEmail,
                            d.getInstitute().getUniversity().getId());
                    return subjectInDirectionRepository.findByDirectionId(directionId);
                }
                AdminQueryScope scope = universityScopeService.resolveAdminQueryScope(viewerEmail, universityId);
                if (scope.globalAllUniversities()) {
                    return subjectInDirectionRepository.findAll();
                }
                return subjectInDirectionRepository.findByDirection_Institute_University_Id(scope.universityId());
            }
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
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                Long uni = entity.getDirection().getInstitute().getUniversity().getId();
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            }
        });
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public SubjectInDirectionResponse create(SubjectInDirectionRequest request, String adminEmail) {
        StudyDirection direction = studyDirectionRepository.findById(request.getDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + request.getDirectionId()));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                direction.getInstitute().getUniversity().getId());
        universityScopeService.assertStudyDirectionInUniversity(request.getDirectionId(), uni);

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
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
        SubjectInDirection entity = subjectInDirectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                entity.getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertSubjectDirectionInUniversity(id, uni);
        StudyDirection newDir = studyDirectionRepository.findById(request.getDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + request.getDirectionId()));
        universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                newDir.getInstitute().getUniversity().getId());
        universityScopeService.assertStudyDirectionInUniversity(request.getDirectionId(),
                newDir.getInstitute().getUniversity().getId());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.getSubjectId()));
        entity.setSubject(subject);
        entity.setDirection(newDir);
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
        SubjectInDirection entity = subjectInDirectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                entity.getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertSubjectDirectionInUniversity(id, uni);
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
