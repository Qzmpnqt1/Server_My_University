package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectPracticeRequest;
import org.example.dto.response.SubjectPracticeResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.SubjectInDirection;
import org.example.model.SubjectPractice;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.SubjectInDirectionRepository;
import org.example.repository.SubjectPracticeRepository;
import org.example.repository.UsersRepository;
import org.example.service.SubjectPracticeService;
import org.example.service.UniversityScopeService;
import org.example.util.RussianSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectPracticeServiceImpl implements SubjectPracticeService {

    private final SubjectPracticeRepository subjectPracticeRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<SubjectPracticeResponse> getBySubjectDirection(Long subjectDirectionId, String viewerEmail) {
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireCampusUniversityId(viewerEmail);
                universityScopeService.assertSubjectDirectionInUniversity(subjectDirectionId, uni);
            } else if (u.getUserType() == UserType.SUPER_ADMIN) {
                SubjectInDirection sd = subjectInDirectionRepository.findById(subjectDirectionId)
                        .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + subjectDirectionId));
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail,
                        sd.getDirection().getInstitute().getUniversity().getId());
            }
        });
        return subjectPracticeRepository.findBySubjectDirectionId(subjectDirectionId).stream()
                .map(this::mapToResponse)
                .sorted(RussianSort.subjectPracticesByNumber())
                .collect(Collectors.toList());
    }

    @Override
    public SubjectPracticeResponse getById(Long id, String viewerEmail) {
        SubjectPractice entity = subjectPracticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectPractice not found with id: " + id));
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN) {
                Long uni = universityScopeService.requireCampusUniversityId(viewerEmail);
                universityScopeService.assertSubjectDirectionInUniversity(entity.getSubjectDirection().getId(), uni);
            } else if (u.getUserType() == UserType.SUPER_ADMIN) {
                Long uni = entity.getSubjectDirection().getDirection().getInstitute().getUniversity().getId();
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            }
        });
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public SubjectPracticeResponse create(SubjectPracticeRequest request, String adminEmail) {
        SubjectInDirection subjectDirection = subjectInDirectionRepository.findById(request.getSubjectDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + request.getSubjectDirectionId()));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                subjectDirection.getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertSubjectDirectionInUniversity(request.getSubjectDirectionId(), uni);
        SubjectPractice entity = SubjectPractice.builder()
                .subjectDirection(subjectDirection)
                .practiceNumber(request.getPracticeNumber())
                .practiceTitle(request.getPracticeTitle())
                .maxGrade(request.getMaxGrade())
                .isCredit(request.getIsCredit())
                .build();
        return mapToResponse(subjectPracticeRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectPracticeResponse update(Long id, SubjectPracticeRequest request, String adminEmail) {
        SubjectPractice entity = subjectPracticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectPractice not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                entity.getSubjectDirection().getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertSubjectDirectionInUniversity(entity.getSubjectDirection().getId(), uni);
        SubjectInDirection subjectDirection = subjectInDirectionRepository.findById(request.getSubjectDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + request.getSubjectDirectionId()));
        universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                subjectDirection.getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertSubjectDirectionInUniversity(request.getSubjectDirectionId(),
                subjectDirection.getDirection().getInstitute().getUniversity().getId());
        entity.setSubjectDirection(subjectDirection);
        entity.setPracticeNumber(request.getPracticeNumber());
        entity.setPracticeTitle(request.getPracticeTitle());
        entity.setMaxGrade(request.getMaxGrade());
        entity.setIsCredit(request.getIsCredit());
        return mapToResponse(subjectPracticeRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        SubjectPractice entity = subjectPracticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectPractice not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                entity.getSubjectDirection().getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertSubjectDirectionInUniversity(entity.getSubjectDirection().getId(), uni);
        subjectPracticeRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private SubjectPracticeResponse mapToResponse(SubjectPractice entity) {
        return SubjectPracticeResponse.builder()
                .id(entity.getId())
                .subjectDirectionId(entity.getSubjectDirection().getId())
                .practiceNumber(entity.getPracticeNumber())
                .practiceTitle(entity.getPracticeTitle())
                .maxGrade(entity.getMaxGrade())
                .isCredit(entity.getIsCredit())
                .build();
    }
}
