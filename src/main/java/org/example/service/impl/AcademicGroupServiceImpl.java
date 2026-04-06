package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.AcademicGroupRequest;
import org.example.dto.response.AcademicGroupResponse;
import org.example.exception.ResourceNotFoundException;
import org.example.model.AcademicGroup;
import org.example.model.StudyDirection;
import org.example.model.UserType;
import org.example.model.Users;
import org.example.repository.AcademicGroupRepository;
import org.example.repository.StudyDirectionRepository;
import org.example.repository.UsersRepository;
import org.example.service.AcademicGroupService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicGroupServiceImpl implements AcademicGroupService {

    private final AcademicGroupRepository academicGroupRepository;
    private final StudyDirectionRepository studyDirectionRepository;
    private final UsersRepository usersRepository;
    private final UniversityScopeService universityScopeService;

    @Override
    public List<AcademicGroupResponse> getAll(Long directionId, String viewerEmail) {
        Optional<Users> viewer = resolveViewer(viewerEmail);
        if (viewer.isPresent()) {
            UserType t = viewer.get().getUserType();
            if (t == UserType.ADMIN) {
                Long uni = universityScopeService.requireCampusUniversityId(viewerEmail);
                if (directionId != null) {
                    universityScopeService.assertStudyDirectionInUniversity(directionId, uni);
                    return academicGroupRepository.findByDirectionId(directionId).stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                }
                return academicGroupRepository.findByDirection_Institute_University_Id(uni).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
            if (t == UserType.SUPER_ADMIN) {
                if (directionId != null) {
                    StudyDirection d = studyDirectionRepository.findById(directionId)
                            .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + directionId));
                    universityScopeService.enforceAccessToEntityUniversity(viewerEmail,
                            d.getInstitute().getUniversity().getId());
                    return academicGroupRepository.findByDirectionId(directionId).stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                }
                return academicGroupRepository.findAll().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
        }
        List<AcademicGroup> groups = (directionId != null)
                ? academicGroupRepository.findByDirectionId(directionId)
                : academicGroupRepository.findAll();
        return groups.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AcademicGroupResponse getById(Long id, String viewerEmail) {
        AcademicGroup group = academicGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic group not found with id: " + id));
        resolveViewer(viewerEmail).ifPresent(u -> {
            if (u.getUserType() == UserType.ADMIN || u.getUserType() == UserType.SUPER_ADMIN) {
                Long uni = group.getDirection().getInstitute().getUniversity().getId();
                universityScopeService.enforceAccessToEntityUniversity(viewerEmail, uni);
            }
        });
        return mapToResponse(group);
    }

    @Override
    @Transactional
    public AcademicGroupResponse create(AcademicGroupRequest request, String adminEmail) {
        StudyDirection direction = studyDirectionRepository.findById(request.getDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + request.getDirectionId()));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                direction.getInstitute().getUniversity().getId());
        universityScopeService.assertStudyDirectionInUniversity(request.getDirectionId(), uni);
        AcademicGroup group = AcademicGroup.builder()
                .name(request.getName())
                .course(request.getCourse())
                .yearOfAdmission(request.getYearOfAdmission())
                .direction(direction)
                .build();
        return mapToResponse(academicGroupRepository.save(group));
    }

    @Override
    @Transactional
    public AcademicGroupResponse update(Long id, AcademicGroupRequest request, String adminEmail) {
        AcademicGroup group = academicGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic group not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                group.getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertAcademicGroupInUniversity(id, uni);
        StudyDirection newDir = studyDirectionRepository.findById(request.getDirectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Study direction not found with id: " + request.getDirectionId()));
        universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                newDir.getInstitute().getUniversity().getId());
        universityScopeService.assertStudyDirectionInUniversity(request.getDirectionId(),
                newDir.getInstitute().getUniversity().getId());
        group.setName(request.getName());
        group.setCourse(request.getCourse());
        group.setYearOfAdmission(request.getYearOfAdmission());
        group.setDirection(newDir);
        return mapToResponse(academicGroupRepository.save(group));
    }

    @Override
    @Transactional
    public void delete(Long id, String adminEmail) {
        AcademicGroup group = academicGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic group not found with id: " + id));
        Long uni = universityScopeService.enforceAccessToEntityUniversity(adminEmail,
                group.getDirection().getInstitute().getUniversity().getId());
        universityScopeService.assertAcademicGroupInUniversity(id, uni);
        academicGroupRepository.deleteById(id);
    }

    private Optional<Users> resolveViewer(String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank() || "anonymousUser".equals(viewerEmail)) {
            return Optional.empty();
        }
        return usersRepository.findByEmail(viewerEmail);
    }

    private AcademicGroupResponse mapToResponse(AcademicGroup group) {
        return AcademicGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .course(group.getCourse())
                .yearOfAdmission(group.getYearOfAdmission())
                .directionId(group.getDirection().getId())
                .directionName(group.getDirection().getName())
                .build();
    }
}
