package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.SubjectLessonTypeRequest;
import org.example.dto.response.SubjectLessonTypeResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.SubjectInDirection;
import org.example.model.SubjectLessonType;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.SubjectInDirectionRepository;
import org.example.repository.SubjectLessonTypeRepository;
import org.example.repository.UsersRepository;
import org.example.service.SubjectLessonTypeService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectLessonTypeServiceImpl implements SubjectLessonTypeService {

    private final SubjectLessonTypeRepository subjectLessonTypeRepository;
    private final SubjectInDirectionRepository subjectInDirectionRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<SubjectLessonTypeResponse> getAll(Long subjectDirectionId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent() && viewer.get().getUserType() == UserType.ADMIN) {
            Long uni = universityScopeService.requireAdminUniversityId(viewerEmail);
            if (subjectDirectionId != null) {
                universityScopeService.assertSubjectDirectionInUniversity(subjectDirectionId, uni);
                return subjectLessonTypeRepository.findBySubjectDirectionId(subjectDirectionId).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
            return subjectLessonTypeRepository.findByUniversityId(uni).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        List<SubjectLessonType> list = (subjectDirectionId != null)
                ? subjectLessonTypeRepository.findBySubjectDirectionId(subjectDirectionId)
                : subjectLessonTypeRepository.findAll();
        return list.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectLessonTypeResponse create(SubjectLessonTypeRequest request, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        universityScopeService.assertSubjectDirectionInUniversity(request.getSubjectDirectionId(), uni);
        SubjectInDirection subjectDirection = subjectInDirectionRepository.findById(request.getSubjectDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("SubjectInDirection not found with id: " + request.getSubjectDirectionId()));
        SubjectLessonType entity = SubjectLessonType.builder()
                .subjectDirection(subjectDirection)
                .lessonType(request.getLessonType())
                .build();
        return mapToResponse(subjectLessonTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        Long uni = universityScopeService.requireAdminUniversityId(adminEmail);
        SubjectLessonType entity = subjectLessonTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectLessonType not found with id: " + id));
        universityScopeService.assertSubjectDirectionInUniversity(entity.getSubjectDirection().getId(), uni);
        subjectLessonTypeRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private SubjectLessonTypeResponse mapToResponse(SubjectLessonType entity) {
        return SubjectLessonTypeResponse.builder()
                .id(entity.getId())
                .subjectDirectionId(entity.getSubjectDirection().getId())
                .lessonType(entity.getLessonType())
                .build();
    }
}
